package org.jibe77.hermanas.client.email;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.entity.Parameter;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.security.ssrf.DestinationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final ConfigService configService;
    private final ParameterRepository parameterRepository;

    private final List<MimeMessagePreparator> sendingQueue = new ArrayList<>();

    @Value("${email.notification.from:}")
    private String emailNotificationFromDefault;

    /**
     * Domain JavaMail puts in the Message-ID ({@code <…JavaMail.user@host>}). Without it the
     * Pi's own hostname ends up there, which spam filters penalise. Spring Boot only applies
     * {@code spring.mail.properties.*} to its auto-configured sender, not to the one built
     * in {@link #resolveSender()}, hence this explicit copy.
     */
    @Value("${spring.mail.properties.mail.host:}")
    private String mailSessionHost;

    @Autowired(required = false)
    private HermanasUserRepository userRepository;

    /**
     * Optional so the narrow unit tests that build an {@code EmailService} by hand keep
     * working; the Spring context always provides it.
     */
    @Autowired(required = false)
    private DestinationValidator destinationValidator;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender, ConfigService configService,
                       ParameterRepository parameterRepository) {
        this.mailSender = mailSender;
        this.configService = configService;
        this.parameterRepository = parameterRepository;
    }

    /**
     * Resolves the mail sender to use for the next send. Builds a fresh
     * {@link JavaMailSenderImpl} from the dynamic configuration managed by
     * {@link ConfigService} (database first, {@code application.properties} fallback),
     * so that changes made via the admin UI take effect on the very next email
     * without requiring a restart.
     *
     * <p>If {@code ConfigService} returns a blank host (e.g. in a test context where
     * no SMTP is configured), falls back to the Spring Boot auto-configured
     * {@link JavaMailSender} bean that was injected at startup.</p>
     *
     * @return the sender to use, or {@code null} when the configured destination is refused
     *         by the SSRF validator — callers must treat that as "e-mail is off for now",
     *         never as a fatal error.
     */
    protected JavaMailSender resolveSender() {
        String host = configService.getMailHost();
        if (host == null || host.isBlank()) {
            return mailSender;
        }

        // SSRF-VULN-03: the SMTP transport was the least-filtered sink — any host and any
        // port could be stored, and the SMTP credentials are transmitted to whatever host
        // the row names. Re-check on every send, not only when the value is written.
        if (destinationValidator != null) {
            try {
                destinationValidator.validateSmtpDestination(host, configService.getMailPort());
            } catch (DestinationValidator.InvalidDestinationException refused) {
                // A refused relay is a configuration problem, never a reason to take the
                // whole application down. This method sits on the startup path — WifiService
                // drains the sending queue from a @PostConstruct — and on 2026-11-18 a
                // split-horizon mail.r3n4.uk resolving to a LAN address made the door
                // automation fail to boot at all, restart after restart. Email degrades to
                // "not sent"; the coop keeps working.
                logger.error("SMTP relay destination refused, e-mail notifications are off "
                        + "until it is corrected: {}", refused.getMessage());
                return null;
            }
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(configService.getMailPort());
        sender.setUsername(configService.getMailUsername());
        sender.setPassword(configService.getMailPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        boolean auth = configService.isMailSmtpAuth();
        boolean startTls = configService.isMailStartTlsEnable();
        int port = configService.getMailPort();
        // CRYPTO-013: implicit TLS (port 465) and opportunistic STARTTLS (25/587) are
        // different transports. The shipped properties used to set socketFactory.port=465 /
        // socketFactory.class=SSLSocketFactory, which JavaMail's SMTPTransport ignores —
        // the socket came up in plaintext whatever the operator believed.
        boolean implicitTls = port == 465;

        props.put("mail.smtp.auth", String.valueOf(auth));
        if (implicitTls) {
            props.put("mail.smtp.ssl.enable", "true");
            // STARTTLS on an already-encrypted socket is meaningless and confuses some
            // relays; leave it off and say why.
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
            // The whole point of this block: with only `starttls.enable`, STARTTLS is
            // *opportunistic*. A server that does not advertise it — or an on-path
            // adversary that strips the capability out of the EHLO response — let the
            // session continue in plaintext, and `mail.smtp.auth=true` then sent
            // `AUTH LOGIN` in clear text. `required` makes the handshake a precondition
            // for the send instead of a preference.
            props.put("mail.smtp.starttls.required", String.valueOf(startTls));
        }

        if (auth && !implicitTls && !startTls) {
            // Belt and braces: never negotiate credentials onto a socket we know is bare.
            throw new IllegalStateException(
                    "Refusing to send: SMTP authentication is enabled but neither STARTTLS nor"
                            + " implicit TLS is configured. The credentials would cross the wire"
                            + " in plaintext. Enable STARTTLS, use port 465, or turn off auth.");
        }

        props.put("mail.smtp.connectiontimeout", "50000");
        props.put("mail.smtp.timeout", "50000");
        props.put("mail.smtp.writetimeout", "50000");
        if (mailSessionHost != null && !mailSessionHost.isBlank()) {
            props.put("mail.host", mailSessionHost.trim());
        }

        logger.debug("Resolved SMTP sender: host={}, port={}, auth={}, starttls={}, implicit-tls={}",
                host, port, auth, startTls, implicitTls);

        return sender;
    }

    private String getEmailNotificationFrom() {
        Parameter row = parameterRepository.findByEntryKey("email.notification.from");
        if (row != null && row.getEntryValue() != null && !row.getEntryValue().trim().isEmpty()) {
            return row.getEntryValue().trim();
        }
        if (emailNotificationFromDefault != null && !emailNotificationFromDefault.trim().isEmpty()) {
            return emailNotificationFromDefault.trim();
        }
        return null;
    }

    public void sendMail(String subject, String body, Optional<File>... filesToAttach) {
        sendMailTo(resolveRecipients(), subject, body, filesToAttach);
    }

    /**
     * Sends a mail only to users with the {@code ADMIN} role and notifications enabled.
     * Used by the Actuator health-check so that diagnostic emails do not spam every
     * opted-in user on the instance.
     */
    @SafeVarargs
    public final void sendMailToAdmins(String subject, String body,
                                      Optional<File>... filesToAttach) {
        sendMailTo(resolveAdminRecipients(), subject, body, filesToAttach);
    }

    /**
     * Sends a mail to an explicit list of recipients, bypassing the opt-in resolution. Used for
     * one-off transactional mails such as registration confirmation and the admin notification of
     * a pending account.
     *
     * <p><strong>Contract (XSS-VULN-01):</strong> {@code htmlBody} is a <em>trusted template</em>.
     * It is written verbatim into the {@code text/html} MIME part, so anything interpolated into
     * it must already be escaped — see {@link #escapeHtml(String)}. Any caller whose body is
     * plain text — which is every notification built from device or model output — must use
     * {@link #sendPlainTextMailTo} instead, which escapes at this boundary so no call-site can
     * inherit the flaw.</p>
     */
    @SafeVarargs
    public final void sendMailTo(List<String> recipients, String subject, String htmlBody,
                                 Optional<File>... filesToAttach) {
        // multipart/alternative with a text/plain part: HTML-only mails score as spam.
        sendInternal(recipients, subject, htmlToPlainText(htmlBody), htmlBody, filesToAttach);
    }

    /**
     * Plain-text counterpart of {@link #sendMailTo}. The body is HTML-escaped here, before it
     * can reach any MIME part, and line breaks become {@code <br>}.
     *
     * <p>This is the fix for the second-order HTML injection: the door-verification mail used to
     * interpolate the raw {@code choices[0].message.content} returned by whatever host
     * {@code ai.inference.url} names straight into the {@code text/html} part, so a hostile
     * inference reply could inject markup into a mail the operator's mail client renders.</p>
     */
    @SafeVarargs
    public final void sendPlainTextMailTo(List<String> recipients, String subject, String plainText,
                                          Optional<File>... filesToAttach) {
        String safe = escapeHtml(plainText);
        String html = safe.replace("\r\n", "<br>").replace("\n", "<br>");
        sendInternal(recipients, subject, plainText, html, filesToAttach);
    }

    @SafeVarargs
    private final void sendInternal(List<String> recipients, String subject, String textPart,
                                    String htmlPart, Optional<File>... filesToAttach) {
        if (recipients == null || recipients.isEmpty()) {
            logger.info("No recipients for mail '{}' — skipping.", subject);
            return;
        }
        String from = getEmailNotificationFrom();
        if (from == null) {
            logger.warn("Skipping mail '{}': email.notification.from is not set.", subject);
            return;
        }
        MimeMessagePreparator preparator = mimeMessage -> {
            InternetAddress[] to = recipients.stream()
                    .map(address -> {
                        try {
                            return new InternetAddress(address);
                        } catch (jakarta.mail.internet.AddressException e) {
                            logger.warn("Skipping invalid recipient address '{}'.", address);
                            return null;
                        }
                    })
                    .filter(a -> a != null)
                    .toArray(InternetAddress[]::new);
            mimeMessage.setRecipients(Message.RecipientType.TO, to);
            mimeMessage.setFrom(new InternetAddress(from));
            mimeMessage.setSubject(subject, "UTF-8");

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            if (filesToAttach != null) {
                for (Optional<File> fileToAttach : filesToAttach) {
                    if (fileToAttach != null && fileToAttach.isPresent()) {
                        FileSystemResource file = new FileSystemResource(fileToAttach.get());
                        helper.addAttachment(file.getFilename(), file);
                    }
                }
            }

            // text/plain + text/html → multipart/alternative.
            helper.setText(textPart, htmlPart);
        };
        sendingQueue.add(preparator);
        processSendingQueue();
    }

    /**
     * HTML-escapes a value destined for an HTML MIME part. Centralised so there is exactly one
     * encoder on the mail path and a call-site cannot pick a weaker one.
     */
    public static String escapeHtml(String value) {
        return org.springframework.web.util.HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    /**
     * Resolves the list of recipients for an automated notification: every user in the
     * {@code hermanas_user} table with {@code notificationsEnabled=true} and a non-blank
     * email address. Returns an empty list when no one is opted in — the queued send
     * path logs that fact and skips the message rather than fail.
     */
    private List<String> resolveRecipients() {
        if (userRepository == null) {
            return Collections.emptyList();
        }
        try {
            return userRepository.findByNotificationsEnabledTrue().stream()
                    .map(HermanasUser::getEmail)
                    .filter(e -> e != null && !e.trim().isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to load opted-in users from database; notification will be skipped.", e);
            return Collections.emptyList();
        }
    }

    /**
     * Resolves the list of recipients restricted to {@code ADMIN} users who have
     * notifications enabled and a non-blank email address.
     */
    private List<String> resolveAdminRecipients() {
        if (userRepository == null) {
            return Collections.emptyList();
        }
        try {
            return userRepository.findByRoleAndNotificationsEnabledTrue("ADMIN").stream()
                    .map(HermanasUser::getEmail)
                    .filter(e -> e != null && !e.trim().isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Failed to load admin users from database; notification will be skipped.", e);
            return Collections.emptyList();
        }
    }

    public synchronized void processSendingQueue() {
        logger.info("start processing sending queue.");
        JavaMailSender sender = resolveSender();
        if (sender == null) {
            // Refused destination (see resolveSender). The messages stay queued: a relay
            // that is temporarily unreachable, or blocked until the operator adds it to
            // hermanas.security.allowed-destination-hosts, must not silently eat the
            // notifications that piled up while it was down.
            logger.warn("Sending queue left untouched: no usable SMTP destination.");
            return;
        }
        Iterator<MimeMessagePreparator> it = sendingQueue.iterator();
        while (it.hasNext()) {
            try {
                sender.send(it.next());
                it.remove();
            } catch (MailException ex) {
                logger.error("Can't send email", ex);
            }
        }
        logger.info("sending queue has been processed.");
    }

    /**
     * Sends a synchronous test email and propagates any failure to the caller.
     *
     * <p>Bypasses the regular {@link #sendingQueue} so that the diagnostics UI can show whether
     * the SMTP configuration actually works (auth, TLS, From address allowed by the relay, etc.)
     * instead of silently swallowing the error like the queued path.</p>
     *
     * <p>When {@code picture} is present, it is embedded inline (CID) inside an HTML body so
     * the recipient sees the chicken-coop snapshot directly in the mail body, matching the
     * style of door open/close notifications.</p>
     *
     * @param picture optional snapshot to embed in the body
     * @param recipient explicit destination address — typically the email of the
     *                  authenticated admin clicking the "send test" button.
     * @throws IllegalStateException if the recipient is blank.
     * @throws MailException if the underlying SMTP send fails.
     */
    public void sendTestMail(Optional<File> picture, String recipient) {
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalStateException("No recipient email available for the test. Set your email address in your user profile.");
        }
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipient);
            helper.setFrom(getEmailNotificationFrom());
            helper.setSubject("Hermanas — test email");

            String introText = "This is a test email triggered from the diagnostics panel. "
                    + "If you read this, the SMTP configuration works.";
            if (picture.isPresent()) {
                String html = "<p>" + introText + "</p>"
                        + "<p>Latest chicken-coop snapshot:</p>"
                        + "<p><img src=\"cid:coopPicture\" style=\"max-width:100%;height:auto;\" /></p>";
                // text/plain + text/html → multipart/alternative, puis addInline wrappera en multipart/related
                helper.setText(introText + "\n(See attached image for chicken-coop snapshot.)", html);
                helper.addInline("coopPicture", picture.get());
            } else {
                helper.setText(introText + "\n(No picture available.)", false);
            }
        };
        logger.info("Sending diagnostics test email to {}.", recipient);
        JavaMailSender sender = resolveSender();
        if (sender == null) {
            // Contrairement au chemin file d'attente, celui-ci est declenche par un admin
            // qui clique sur « envoyer un test » : il faut lui dire pourquoi rien ne part.
            throw new IllegalStateException("The configured SMTP destination was refused by "
                    + "the destination validator; the application log gives the reason and the "
                    + "property to adjust.");
        }
        sender.send(preparator);
        logger.info("Diagnostics test email sent successfully.");
    }

    /**
     * Plain-text rendering of an HTML body for the text/plain alternative: block-level
     * tags become line breaks, the remaining tags are dropped and the common entities
     * decoded. A body that is already plain text comes out unchanged.
     */
    static String htmlToPlainText(String html) {
        if (html == null) {
            return "";
        }
        String text = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|h[1-6]|li|tr|table)>", "\n")
                .replaceAll("(?i)<(ul|ol)[^>]*>", "\n")
                .replaceAll("(?i)<li[^>]*>", "- ")
                .replaceAll("(?s)<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
        return text.replaceAll("[ \\t]+\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    public boolean isSendingQueueEmpty() {
        return sendingQueue.isEmpty();
    }

    public void emptySendingQueue() {
        sendingQueue.clear();
    }
}
