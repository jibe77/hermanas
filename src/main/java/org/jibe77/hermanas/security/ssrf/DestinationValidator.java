package org.jibe77.hermanas.security.ssrf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single gate in front of every outbound request the application makes on behalf of
 * something an operator (or an attacker holding a low-privilege session) can write
 * (SSRF-VULN-03, SSRF-VULN-01).
 *
 * <p>Four configuration setters used to accept an attacker-chosen destination and persist
 * it with no validation, and nothing re-checked the value when it was read back out of the
 * database: {@code ConfigService.setWeatherInfoUrl} rejected only blank, its own Javadoc
 * saying "we do not validate that here"; {@code setAiInferenceUrl} trimmed; {@code
 * setMailHost} rejected only blank and {@code setMailPort} accepted the whole 1..65535
 * range. The stored row then drove a live socket on every dashboard poll and every
 * scheduler tick, so a single write permanently redirected the server's egress.</p>
 *
 * <h3>What is checked</h3>
 * <ul>
 *   <li><b>Scheme allowlist</b> — {@code https} always, {@code http} only while
 *       {@code hermanas.security.allow-insecure-http-destinations} is left on.</li>
 *   <li><b>Port allowlist</b> — {@code hermanas.security.allowed-destination-ports}.</li>
 *   <li><b>Resolved address</b> — every {@link InetAddress} the hostname resolves to must
 *       not be loopback, site-local, link-local, any-local or multicast. Resolving and
 *       checking <em>every</em> answer is what closes the {@code A → 127.0.0.1} round-robin
 *       trick.</li>
 *   <li><b>No URI-template placeholder for the secret</b> — a destination that is not an
 *       explicitly trusted provider may not contain {@code {key}}, because Spring expands
 *       it and would hand the configured API key to whatever host the caller named.</li>
 * </ul>
 *
 * <h3>Call it twice</h3>
 * <p>Every setter calls it, and every sink calls it again immediately before opening the
 * socket. The setter check gives the operator a clean 400; the sink check is what makes a
 * value already in the database — or one that changes its DNS answer between the two
 * lookups — useless to an attacker.</p>
 *
 * <h3>The escape hatch</h3>
 * <p>{@code hermanas.security.allowed-destination-hosts} is the operator's explicit list of
 * permitted provider hosts. Entries there skip the private-range rule and may carry
 * {@code {key}}. That is how a legitimate inference server on the LAN
 * ({@code alyssa:8080} on the reference deployment) keeps working without turning the
 * check off for everything else. {@code hermanas.security.allow-private-destinations}
 * disables the address rule globally and exists only for development.</p>
 */
@Component
public class DestinationValidator {

    /** Which outbound path is asking, used in the log line so a refusal is traceable. */
    public enum Sink {
        WEATHER("weather provider"),
        AI_INFERENCE("AI inference server"),
        SMTP("SMTP relay"),
        WEB_PUSH("web-push service"),
        DIAGNOSTIC("weather diagnostics");

        private final String label;

        Sink(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(DestinationValidator.class);

    private final Set<String> allowedHosts = new LinkedHashSet<>();
    private final Set<Integer> allowedPorts = new LinkedHashSet<>();

    @Value("${hermanas.security.allowed-destination-hosts:api.openweathermap.org}")
    private String allowedHostsRaw = "api.openweathermap.org";

    @Value("${hermanas.security.allowed-destination-ports:25,80,443,465,587,8080,8443,11434}")
    private String allowedPortsRaw = "25,80,443,465,587,8080,8443,11434";

    @Value("${hermanas.security.allow-private-destinations:false}")
    private boolean allowPrivateDestinations = false;

    @Value("${hermanas.security.allow-insecure-http-destinations:true}")
    private boolean allowInsecureHttp = true;

    @PostConstruct
    void init() {
        for (String host : csv(allowedHostsRaw)) {
            allowedHosts.add(host.toLowerCase(Locale.ROOT));
        }
        for (String port : csv(allowedPortsRaw)) {
            try {
                allowedPorts.add(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                logger.warn("Ignoring malformed entry in hermanas.security.allowed-destination-ports: {}", port);
            }
        }
        logger.info("Destination validation active: hosts={}, ports={}, private destinations {},"
                        + " cleartext http {}.",
                allowedHosts, allowedPorts,
                allowPrivateDestinations ? "ALLOWED" : "rejected",
                allowInsecureHttp ? "allowed" : "rejected");
    }

    /**
     * Validates an absolute HTTP(S) URL that the caller is about to fetch.
     *
     * @throws InvalidDestinationException when the destination must not be dialled
     */
    public void validateHttpUrl(String url, Sink sink) {
        if (url == null || url.trim().isEmpty()) {
            throw new InvalidDestinationException("A destination URL is required.");
        }
        URI uri = parse(url.trim(), sink);
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme)) {
            if (!"http".equals(scheme)) {
                throw reject(sink, url, "scheme '" + scheme + "' is not allowed");
            }
            if (!allowInsecureHttp) {
                throw reject(sink, url, "cleartext http is refused, use https");
            }
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw reject(sink, url, "no hostname");
        }
        int port = uri.getPort() != -1 ? uri.getPort() : ("https".equals(scheme) ? 443 : 80);
        boolean trusted = isTrustedHost(host);
        if (!trusted && !allowedPorts.contains(port)) {
            throw reject(sink, url, "port " + port + " is not on the allowlist");
        }
        // The configured provider key must never be interpolated into a URL we do not trust.
        if (!trusted && (url.contains("{key}") || url.contains("{KEY}"))) {
            throw reject(sink, url, "the {key} placeholder is refused for non-allowlisted hosts");
        }
        checkAddresses(host, url, sink, trusted);
    }

    /**
     * Validates an SMTP transport destination. The host is a bare hostname (no scheme), so
     * it goes through the same address rules with the port supplied separately.
     */
    public void validateSmtpDestination(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            throw new InvalidDestinationException("An SMTP host is required.");
        }
        String bare = host.trim();
        // Tolerate an operator pasting "smtp://host" or "host:port".
        int schemeSplit = bare.indexOf("://");
        if (schemeSplit > 0) {
            bare = bare.substring(schemeSplit + 3);
        }
        if (bare.indexOf('/') >= 0) {
            throw reject(Sink.SMTP, host, "must be a hostname, not a URL");
        }
        if (bare.contains(":")) {
            String[] parts = bare.split(":");
            bare = parts[0];
        }
        if (bare.isEmpty()) {
            throw reject(Sink.SMTP, host, "no hostname");
        }
        boolean trusted = isTrustedHost(bare);
        if (!trusted && !allowedPorts.contains(port)) {
            throw reject(Sink.SMTP, host, "port " + port + " is not on the allowlist");
        }
        checkAddresses(bare, host, Sink.SMTP, trusted);
    }

    /** True when the operator explicitly listed this host (exact match or parent domain). */
    public boolean isTrustedHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        // Strip a trailing dot so "example.com." matches "example.com".
        if (h.endsWith(".")) {
            h = h.substring(0, h.length() - 1);
        }
        for (String allowed : allowedHosts) {
            if (h.equals(allowed) || h.endsWith("." + allowed)) {
                return true;
            }
        }
        return false;
    }

    private void checkAddresses(String host, String url, Sink sink, boolean trusted) {
        if (trusted || allowPrivateDestinations) {
            return;
        }
        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw reject(sink, url, "hostname does not resolve");
        }
        for (InetAddress address : addresses) {
            String reason = refusalReason(address);
            if (reason != null) {
                throw reject(sink, url, "resolves to a " + reason + " address (" + address.getHostAddress() + ")");
            }
        }
    }

    /** Returns a human-readable reason for refusing this address, or null when it is fine. */
    private static String refusalReason(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return "loopback";
        }
        if (address.isAnyLocalAddress()) {
            return "any-local";
        }
        if (address.isLinkLocalAddress()) {
            // Covers 169.254.0.0/16, which is where the cloud metadata endpoints live.
            return "link-local";
        }
        if (address.isSiteLocalAddress()) {
            return "private";
        }
        if (address.isMulticastAddress()) {
            return "multicast";
        }
        // 100.64.0.0/10 (carrier-grade NAT) and 198.18.0.0/15 (benchmarking) are not
        // covered by the helpers above but are not reachable public destinations either.
        byte[] raw = address.getAddress();
        if (raw.length == 4) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            if (b0 == 100 && (b1 & 0xC0) == 64) {
                return "carrier-grade NAT";
            }
            if (b0 == 198 && (b1 == 18 || b1 == 19)) {
                return "benchmark";
            }
            if (b0 == 0) {
                return "unspecified";
            }
        }
        return null;
    }

    private URI parse(String url, Sink sink) {
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw reject(sink, url, "is not a well-formed URL");
        }
        if (uri.getScheme() == null) {
            throw reject(sink, url, "is not an absolute URL");
        }
        return uri;
    }

    private static InvalidDestinationException reject(Sink sink, String url, String reason) {
        // Never echo more than the caller already knew, but make the refusal greppable.
        logger.warn("Refusing {} destination {}: {}", sink.label(), abbreviate(url), reason);
        return new InvalidDestinationException(
                "Destination " + abbreviate(url) + " is not allowed for the " + sink.label() + ": " + reason);
    }

    private static String abbreviate(String url) {
        if (url == null) {
            return "<null>";
        }
        return url.length() <= 120 ? url : url.substring(0, 117) + "...";
    }

    private static List<String> csv(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** Thrown when a destination fails validation. */
    public static class InvalidDestinationException extends IllegalArgumentException {
        public InvalidDestinationException(String message) {
            super(message);
        }
    }
}
