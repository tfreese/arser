package de.freese.arser.connector;

import java.net.URI;
import java.util.Objects;

import de.freese.arser.core.component.AbstractComponent;

/**
 * @author Thomas Freese
 */
public abstract class AbstractConnector extends AbstractComponent implements Connector {
    private final URI uri;

    protected AbstractConnector(final URI uri) {
        super();

        this.uri = Objects.requireNonNull(uri, "URI required");
    }

    protected URI getUri() {
        return uri;
    }
}
