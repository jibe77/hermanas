package org.jibe77.hermanas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * La clé remise à {@code PersistentTokenBasedRememberMeServices} (CRYPTO-001).
 *
 * <p>Commençons par ce que cette clé n'est pas : elle ne sert pas à forgér un cookie
 * remember-me. Avec la stratégie « persistent token », le cookie vaut
 * {@code base64(serie:token)} où serie et token sont deux tirages {@code SecureRandom} de
 * 16 octets conservés côté serveur dans {@code persistent_logins} ; la clé n'entre dans le
 * calcul de rien du tout, contrairement à la stratégie sans état
 * {@code TokenBasedRememberMeServices} qui la hache dans la valeur. Forger un cookie
 * suppose de lire une ligne de {@code persistent_logins}, pas de connaître la clé.</p>
 *
 * <p>Ce qui restait était donc de l'hygiène, et elle comptait quand même : une valeur connue
 * voyageait dans le dépôt depuis le commit de baseline {@code 528ee1b}, rien n'affirmait au
 * démarrage que l'exploitant l'avait changée, et elle ne tournait jamais. Le littéral est
 * parti, la valeur vient maintenant de l'environnement.</p>
 *
 * <h3>Pourquoi une clé aléatoire plutôt qu'un refus de démarrer</h3>
 * <p>Le rapport proposait de faire échouer le démarrage quand la valeur est vide ou égale au
 * défaut connu. Ce choix est écarté, et assumé : la clé n'ayant aucune valeur cryptographique
 * ici, un refus de démarrer ne protège rien — mais il laisse le poulailler avec une porte
 * bloquée jusqu'à ce que quelqu'un pose un écran dessus. À la place, une clé de 32 octets est
 * tirée à chaque démarrage, et un WARN le dit. Le coût est nul (la clé n'entre dans aucun
 * calcul, donc aucune session remember-me n'est invalidée par le changement), et le résultat
 * est strictement meilleur : la constante connue ne sert plus jamais, où que ce soit.</p>
 *
 * <p>Pour un déploiement qui tient à une clé stable — par exemple pour partager le même
 * contexte d'authentification entre plusieurs instances — il suffit de poser
 * {@code HERMANAS_SECURITY_REMEMBER_ME_KEY} dans l'environnement, ou la même propriété dans
 * {@code /var/lib/hermanas/application.properties}.</p>
 */
@Component
public class RememberMeKey {

    private static final Logger logger = LoggerFactory.getLogger(RememberMeKey.class);

    /** La valeur qui a voyagé dans le dépôt depuis {@code 528ee1b}. Jamais utilisable. */
    static final String KNOWN_DEFAULT = "change-me-in-production-please";

    private final String value;

    public RememberMeKey(@Value("${hermanas.security.remember-me-key:}") String configured) {
        this.value = resolve(configured);
    }

    /** La clé à remettre aux services remember-me. Jamais vide. */
    public String value() {
        return value;
    }

    private static String resolve(String configured) {
        String trimmed = configured == null ? "" : configured.trim();
        if (!trimmed.isEmpty() && !KNOWN_DEFAULT.equals(trimmed)) {
            return trimmed;
        }
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String generated = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        logger.warn("Remember-me : aucune clé exploitable n'est configurée ({}). Une clé "
                        + "aléatoire est tirée à chaque démarrage. Sans conséquence sur les "
                        + "cookies existants — la clé n'entre pas dans leur calcul — mais "
                        + "HERMANAS_SECURITY_REMEMBER_ME_KEY est attendu en production.",
                trimmed.isEmpty() ? "vide" : "valeur par défaut du dépôt");
        return generated;
    }
}
