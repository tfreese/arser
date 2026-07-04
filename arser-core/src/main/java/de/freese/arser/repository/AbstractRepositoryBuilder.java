package de.freese.arser.repository;

import java.net.URI;

import de.freese.arser.utils.AbstractBuilder;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepositoryBuilder<B, T extends Repository> extends AbstractBuilder<B, T> implements RepositoryBuilder<B, T> {

    private boolean logging;
    private String name;
    private URI uri;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public B name(final String name) {
        this.name = name;

        return self();
    }

    @Override
    public B uri(final URI uri) {
        this.uri = uri;

        return self();
    }

    @Override
    public B withLogging() {
        this.logging = true;

        return self();
    }

    protected URI getUri() {
        return uri;
    }

    protected boolean isLogging() {
        return logging;
    }
}
