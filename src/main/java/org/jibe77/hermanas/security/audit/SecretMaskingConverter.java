package org.jibe77.hermanas.security.audit;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convertis Logback qui caviarde les secrets traversant une ligne de journal
 * ({@code %mask}), utilisé par les appendeurs d'audit (CRYPTO-006).
 *
 * <p>La première ligne de défense est côté émetteur : {@code ConfigService} passe déjà
 * toute écriture de configuration par {@code SecretConfigKeys.render}, qui remplace la
 * valeur d'une clé sensible par {@code <set, len=n>}. Ce convertisseur est la ceinture et
 * les bretelles : une trace empilée qui cite une URL JDBC, un {@code logger.debug} ajouté
 * six mois plus tard, un client SMTP qui recopie la commande AUTH dans son exception — tout
 * cela partait tel quel sur le disque, dans un fichier que n'importe quel lecteur de sauvegarde
 * peut consulter.</p>
 *
 * <p>Le motif vise {@code cle=valeur} et {@code cle: valeur} pour les noms de clés connus.
 * Il ne prétend pas reconnaître un secret arbitraire ; il rend illisible ce qui ressemble à
 * un secret dans une ligne d'audit, ce qui est exactement le vocabulaire que l'application
 * emploie déjà.</p>
 *
 * <p>Limite assumée : seul le message formaté est filtré. Une stack trace intégrale est
 * écrite par Logback après le message et n'est pas réécrite ici — la caviarder ligne par
 * ligne coûterait plus qu'elle ne rapporte sur un Pi, et les exceptions de cause racine
 * contenant un mot de passe sont déjà neutralisées à la source par
 * {@code GlobalExceptionHandler}.</p>
 */
public class SecretMaskingConverter extends ClassicConverter {

    /** {@code cle = valeur} / {@code cle: valeur} pour les noms de clés connus. */
    private static final Pattern SECRET_IN_TEXT = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret|token|api[-_]?key|apikey|appid|appkey"
                    + "|access[-_]?key|private[-_]?key)(\\s*[=:]\\s*)(\\S+)");

    private static final String REDACTED = "$1$2***";

    @Override
    public String convert(ILoggingEvent event) {
        return mask(event.getFormattedMessage());
    }

    /** Visible pour les tests : caviardage pur, sans dépendre de Logback. */
    static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        Matcher matcher = SECRET_IN_TEXT.matcher(message);
        if (!matcher.find()) {
            return message;
        }
        return matcher.replaceAll(REDACTED);
    }
}
