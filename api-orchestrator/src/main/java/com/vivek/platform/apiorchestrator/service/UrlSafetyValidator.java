package com.vivek.platform.apiorchestrator.service;

import com.vivek.platform.apiorchestrator.config.AppProperties;
import com.vivek.platform.apiorchestrator.exception.UnsafeUrlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Outbound-request safety policy.
 *
 * <p>This application fetches arbitrary user-supplied URLs — that is its entire purpose — so it
 * is a server-side request forgery primitive by construction. The point of this class is not to
 * pretend otherwise but to make the policy explicit and configurable:
 *
 * <ul>
 *   <li>only the schemes in {@code app.security.allowed-schemes} are dialled;</li>
 *   <li>hosts in {@code app.security.blocked-hosts} are always refused (cloud metadata
 *       endpoints are blocked by default);</li>
 *   <li>when {@code app.security.allow-private-networks} is false, any host that resolves to a
 *       loopback, link-local, site-local, any-local or multicast address is refused.</li>
 * </ul>
 *
 * <p>The default profile leaves private networks reachable because the common local use of this
 * tool is poking at a service on localhost. The {@code prod} profile turns that off. Deploying
 * with private networks allowed means anyone who can reach this API can reach anything the
 * container can reach.
 *
 * <p>Known limitation, stated plainly: the check resolves DNS once and the HTTP client resolves
 * it again, so a name that flips between a public and a private address in between (DNS
 * rebinding) can slip past. Closing that needs a connection-level check inside the client.
 */
@Component
public class UrlSafetyValidator {

    private static final Logger log = LoggerFactory.getLogger(UrlSafetyValidator.class);

    private final AppProperties properties;

    public UrlSafetyValidator(AppProperties properties) {
        this.properties = properties;
    }

    /** @throws UnsafeUrlException when the URL is malformed or refused by policy. */
    public URI validate(String url) {
        URI uri = parse(url);

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        boolean schemeAllowed = properties.getSecurity().getAllowedSchemes().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(scheme));
        if (!schemeAllowed) {
            throw new UnsafeUrlException("Unsupported URL scheme '" + scheme + "'. Allowed: "
                    + properties.getSecurity().getAllowedSchemes());
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeUrlException("URL has no host: " + url);
        }

        boolean blocked = properties.getSecurity().getBlockedHosts().stream()
                .anyMatch(h -> h.equalsIgnoreCase(host));
        if (blocked) {
            throw new UnsafeUrlException("Host '" + host + "' is blocked by app.security.blocked-hosts");
        }

        if (!properties.getSecurity().isAllowPrivateNetworks()) {
            assertNotPrivate(host);
        }

        return uri;
    }

    private URI parse(String url) {
        try {
            URI uri = new URI(url.trim());
            if (!uri.isAbsolute()) {
                throw new UnsafeUrlException("URL must be absolute (include http:// or https://): " + url);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new UnsafeUrlException("Malformed URL: " + url);
        }
    }

    private void assertNotPrivate(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            // Let the HTTP client produce the DNS failure; refusing here would be a worse message.
            log.debug("Could not resolve {} during safety check", host);
            return;
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isAnyLocalAddress()
                    || address.isMulticastAddress()) {
                throw new UnsafeUrlException("Host '" + host + "' resolves to the private address "
                        + address.getHostAddress()
                        + ", which is refused while app.security.allow-private-networks is false");
            }
        }
    }
}
