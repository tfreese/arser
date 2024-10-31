// Created: 31 Okt. 2024
package de.freese.arser.core.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepositoryConfig implements RepositoryConfig {
    private final String name;
    private final URI uri;

    protected AbstractRepositoryConfig(final String name, final URI uri) {
        super();

        this.name = name;
        this.uri = uri;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URI getUri() {
        return uri;
    }
}
