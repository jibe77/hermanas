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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
 * concurrente présente ce token précédent <em>dans la fenêtre de grâce</em>, on la laisse passer
 * sans refaire tourner le token et sans crier au vol. Au-delà de la fenêtre, le comportement
 * strict d'origine reprend : un vrai token volé, rejoué plus tard, est toujours traité comme un
 * vol.</p>
 *
 * <h3>Ce que la version précédente laissait passer (CRYPTO-003)</h3>
 * <p>La fenêtre faisait 30 secondes, la vérification d'expiration du token était court-circuitée
 * sur ce chemin, et surtout le vol n'était jamais signalé : pendant 30 secondes après chaque
 * rotation, la propriété de sécurité qui justifie tout le schéma de Jaspan — « un jeton déjà
 * tourné qui revient, c'est un cookie volé » — était éteinte sans laisser de trace. Trois
 * corrections :</p>
 * <ul>
 *   <li>la fenêtre tombe à 2 secondes : une rafale de requêtes parallèles d'un même chargement
 *       de page se tient là, une réutilisation différée non ;</li>
 *   <li>la vérification d'expiration de Spring est reproduite sur le chemin de grâce — un token
 *       périmé n'ouvre rien, même dans la fenêtre ;</li>
 *   <li>toute réutilisation d'un token déjà tourné est <em>journalisée en WARN</em> avec la
 *       série et l'adresse source, à l'intérieur comme à l'extérieur de la fenêtre. C'est le
 *       signal que l'exploitant doit pouvoir voir, et il manquait complètement.</li>
 * </ul>
 *
 * <p>La rotation elle-même est sérialisée par série : c'est la vraie cause du faux positif, et
 * une fenêtre plus courte n'aurait été qu'un pansement si deux requêtes pouvaient encore tourner
 * le même token en même temps.</p>
 */
public class GraceWindowRememberMeServices extends PersistentTokenBasedRememberMeServices {

    private static final Logger logger =
            LoggerFactory.getLogger(GraceWindowRememberMeServices.class);

    /**
     * Durée pendant laquelle le token précédent d'une série reste accepté. Deux secondes :
     * assez pour la rafale de requêtes parallèles d'un démarrage de SPA, trop court pour
     * qu'une réutilisation différée — le scénario du vol — en bénéficie (CRYPTO-003).
     */
    static final Duration GRACE_WINDOW = Duration.ofSeconds(2);

    private final PersistentTokenRepository tokenRepository;

    /** série -> token qui vient d'être remplacé, avec son horodatage d'expiration de grâce. */
    private final Map<String, SupersededToken> recentlyRotated = new ConcurrentHashMap<>();

    /** Une serrure par série pour que deux requêtes concurrentes ne tournent pas le même token. */
    private final Map<String, Lock> seriesLocks = new ConcurrentHashMap<>();

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

        recentlyRotated.entrySet().removeIf(entry -> entry.getValue().isExpired());

        Lock lock = seriesLocks.computeIfAbsent(series, key -> new ReentrantLock());
        lock.lock();
        try {
            return processWithGraceWindow(series, presentedToken, request, response);
        } finally {
            lock.unlock();
        }
    }

    private UserDetails processWithGraceWindow(String series, String presentedToken,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        SupersededToken superseded = recentlyRotated.get(series);
        if (superseded == null || !superseded.token.equals(presentedToken)) {
            // Cas normal : on retient le token sur le point d'être remplacé, puis on délègue la
            // rotation à la classe mère.
            PersistentRememberMeToken beforeRotation = tokenRepository.getTokenForSeries(series);
            if (beforeRotation != null && beforeRotation.getTokenValue().equals(presentedToken)) {
                recentlyRotated.put(series, new SupersededToken(presentedToken));
            }
            return super.processAutoLoginCookie(new String[] {series, presentedToken}, request,
                    response);
        }

        // Le jeton présenté est un jeton déjà tourné de cette série.
        PersistentRememberMeToken current = tokenRepository.getTokenForSeries(series);
        if (current == null) {
            // La série n'existe plus : rien à authentifier, la classe mère va nettoyer.
            return super.processAutoLoginCookie(new String[] {series, presentedToken}, request,
                    response);
        }
        if (!superseded.isExpired() && !isTokenExpired(current)) {
            // Requête concurrente d'un même chargement de page : le token a déjà été remplacé
            // par une requête sœur. On authentifie sans refaire tourner le token et SANS
            // réécrire le cookie, pour ne pas écraser celui que la requête gagnante a posé.
            logger.debug("Remember-me : token précédent accepté dans la fenêtre de grâce "
                    + "pour la série {} (requêtes concurrentes).", series);
            return getUserDetailsService().loadUserByUsername(current.getUsername());
        }
        if (isTokenExpired(current)) {
            // CRYPTO-003 : la vérification d'expiration de Spring était court-circuitée sur ce
            // chemin. Un token périmé ne doit rien ouvrir, même dans la fenêtre de grâce.
            logger.warn("Remember-me : token de la série {} présenté dans la fenêtre de grâce "
                    + "mais périmé — refus.", series);
        } else {
            // CRYPTO-003 : un token déjà tourné rejoué après la fenêtre est un VOL, pas un faux
            // positif. On l'annonce avant de laisser la classe mère lever CookieTheftException et
            // purger toutes les séries de l'utilisateur.
            logger.warn("Remember-me : rejou d'un token déjà remplacé HORS fenêtre de grâce pour "
                    + "la série {} depuis {} — traité comme un vol de cookie.",
                    series, request.getRemoteAddr());
        }
        return super.processAutoLoginCookie(new String[] {series, presentedToken}, request,
                response);
    }

    /**
     * Même règle que {@code PersistentTokenBasedRememberMeServices.processAutoLoginCookie} :
     * {@code lastUsed + tokenValiditySeconds} dépasse-t-il la fenêtre, le token est mort.
     * Reproduite ici parce que le chemin de grâce court-circuite la méthode mère.
     * Visible pour les tests.
     */
    boolean isTokenExpired(PersistentRememberMeToken token) {
        long maxAge = getTokenValiditySeconds() * 1000L;
        return System.currentTimeMillis() - token.getDate().getTime() > maxAge;
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
                                  org.springframework.security.core.Authentication successfulAuthentication) {
        super.onLoginSuccess(request, response, successfulAuthentication);
        recentlyRotated.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    // Les entrées de seriesLocks ne sont jamais évincées : une entrée = une série de cookie vue
    // depuis le démarrage, soit quelques centaines d'octets au pire sur un système domestique.
    // Les évincer pendant qu'un thread les détient rouvrirait la fenêtre de course que la serrure
    // sert précisément à fermer.

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
