package de.freese.arser.repository;

import java.net.URI;

import de.freese.arser.utils.AbstractBuilder;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepositoryBuilder<B, T> extends AbstractBuilder<B, T> {

    private boolean logging = false;
    private String name;
    private URI uri;

    public B name(final String name) {
        this.name = name;

        return self();
    }

    public B uri(final URI uri) {
        this.uri = uri;

        return self();
    }

    public B withLogging() {
        this.logging = true;

        return self();
    }

    protected String getName() {
        return name;
    }

    protected URI getUri() {
        return uri;
    }

    protected boolean isLogging() {
        return logging;
    }
}
