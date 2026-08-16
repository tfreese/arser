package de.freese.arser.repository;

import java.net.URI;

/**
 * @author Thomas Freese
 * @since 04.07.26
 */
public interface RepositoryBuilder<B, T extends Repository> {
    T build() throws Exception;

    String getName();

    B name(String name);

    B uri(URI uri);

    B withLogging();
}
