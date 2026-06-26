package de.freese.arser.connector.security;

import java.net.URI;
import java.util.Optional;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface CredentialsProvider {
    CredentialsProvider NONE = uri -> Optional.empty();

    Optional<Credentials> resolve(URI uri);
}
