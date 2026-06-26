package de.freese.arser.connector.security;

import java.net.URI;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface UriGuard extends Predicate<URI> {
    UriGuard ALLOW_ALL = uri -> true;

    static UriGuard denyInternalNetworks() {
        return uri -> {
            final String host = uri.getHost();

            if (host == null) {
                return true;
            }

            return !("localhost".equals(host)
                    || host.startsWith("127.")
                    || host.startsWith("169.254.")
                    || host.startsWith("10.")
                    || host.startsWith("192.168."));
        };
    }

    static UriGuard hostAllowlist(final Set<String> hosts) {
        return uri -> uri.getHost() != null && hosts.contains(uri.getHost().toLowerCase());
    }
}
