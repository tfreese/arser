package de.freese.arser.connector;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public abstract class AbstractConnectorBuilder<B> {

    private URI uri;

    public abstract Connector build() throws Exception;

    public B uri(final URI uri) {
        this.uri = uri;

        return self();
    }

    protected URI getUri() {
        return uri;
    }

    protected abstract B self();
}
