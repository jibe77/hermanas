package org.jibe77.hermanas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link PersistentTokenBasedRememberMeServices} tolérant aux requêtes concurrentes.
 *
 * <h3>Le problème</h3>
 * <p>La stratégie « persistent token » fait tourner le token à chaque utilisation : la requête
 * qui présente le cookie reçoit un nouveau token, et l'ancien devient invalide. Spring interprète
 * la réutilisation d'un ancien token comme un vol de cookie ({@link CookieTheftException}).</p>
 *
 * <p>Or le SPA/PWA Hermanas émet <em>plusieurs requêtes en parallèle</em> au démarrage. Toutes
 * partent avec le même cookie : la première fait tourner le token, les suivantes arrivent avec
 * un token déjà périmé et déclenchent un faux positif de vol. La réponse en erreur porte alors
 * un en-tête de <em>suppression</em> du cookie remember-me. Comme le navigateur partage un seul
 * pot de cookies entre ces réponses concurrentes, cette suppression écrase le cookie valide que
 * la première réponse venait d'écrire : le remember-me est détruit et l'utilisateur doit se
 * reconnecter — exactement le symptôme « je dois me réauthentifier après quelques heures ».</p>
 *
 * <h3>La correction</h3>
 * <p>On mémorise brièvement, par série, le token qui vient d'être remplacé. Si une requête
 * concurrente présente ce token précédent dans la fenêtre de grâce, on la laisse passer sans
 * refaire tourner le token et sans crier au vol. Au-delà de la fenêtre, le comportement strict
 * d'origine reprend : un vrai token volé, rejoué plus tard, est toujours traité comme un vol.</p>
 *
 * <p>La fenêtre est volontairement courte (quelques secondes) : elle couvre une rafale de
 * requêtes d'un même chargement de page, pas une réutilisation différée.</p>
 */
public class GraceWindowRememberMeServices extends PersistentTokenBasedRememberMeServices {

    private static final Logger logger =
            LoggerFactory.getLogger(GraceWindowRememberMeServices.class);

    /**
     * Durée pendant laquelle le token précédent d'une série reste accepté. Assez long pour
     * couvrir les requêtes parallèles d'un démarrage de SPA, assez court pour ne pas élargir
     * la surface d'attaque en cas de vol réel.
     */
    static final Duration GRACE_WINDOW = Duration.ofSeconds(30);

    private final PersistentTokenRepository tokenRepository;

    /** série -> token qui vient d'être remplacé, avec son horodatage d'expiration de grâce. */
    private final Map<String, SupersededToken> recentlyRotated = new ConcurrentHashMap<>();

    public GraceWindowRememberMeServices(String key, UserDetailsService userDetailsService,
                                         PersistentTokenRepository tokenRepository) {
        super(key, userDetailsService, tokenRepository);
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request,
                                                 HttpServletResponse response) {
        if (cookieTokens.length != 2) {
            // Laisse la classe mère produire l'InvalidCookieException habituelle.
            return super.processAutoLoginCookie(cookieTokens, request, response);
        }
        String series = cookieTokens[0];
        String presentedToken = cookieTokens[1];

        purgeExpired();

        SupersededToken superseded = recentlyRotated.get(series);
        if (superseded != null && !superseded.isExpired()
                && superseded.token.equals(presentedToken)) {
            // Requête concurrente d'un même chargement de page : le token a déjà été remplacé
            // par une requête sœur. On authentifie sans refaire tourner le token et SANS
            // réécrire le cookie, pour ne pas écraser celui que la requête gagnante a posé.
            PersistentRememberMeToken current = tokenRepository.getTokenForSeries(series);
            if (current != null) {
                logger.debug("Remember-me : token précédent accepté dans la fenêtre de grâce "
                        + "pour la série {} (requêtes concurrentes).", series);
                return getUserDetailsService().loadUserByUsername(current.getUsername());
            }
        }

        // Cas normal : on retient le token sur le point d'être remplacé, puis on délègue la
        // rotation à la classe mère.
        PersistentRememberMeToken beforeRotation = tokenRepository.getTokenForSeries(series);
        if (beforeRotation != null && beforeRotation.getTokenValue().equals(presentedToken)) {
            recentlyRotated.put(series, new SupersededToken(presentedToken));
        }
        return super.processAutoLoginCookie(cookieTokens, request, response);
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
                                  org.springframework.security.core.Authentication successfulAuthentication) {
        super.onLoginSuccess(request, response, successfulAuthentication);
        purgeExpired();
    }

    private void purgeExpired() {
        recentlyRotated.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private static final class SupersededToken {
        private final String token;
        private final long expiresAtNanos;

        SupersededToken(String token) {
            this.token = token;
            this.expiresAtNanos = System.nanoTime() + GRACE_WINDOW.toNanos();
        }

        boolean isExpired() {
            return System.nanoTime() - expiresAtNanos > 0;
        }
    }
}
