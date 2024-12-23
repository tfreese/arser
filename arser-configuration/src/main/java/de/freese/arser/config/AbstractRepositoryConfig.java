// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepositoryConfig implements RepositoryConfig {
    private final String contextRoot;
    private final URI uri;

    protected AbstractRepositoryConfig(final String contextRoot, final URI uri) {
        super();

        this.contextRoot = contextRoot;
        this.uri = uri;
    }

    @Override
    public String getContextRoot() {
        return contextRoot;
    }

    @Override
    public URI getUri() {
        return uri;
    }
}
