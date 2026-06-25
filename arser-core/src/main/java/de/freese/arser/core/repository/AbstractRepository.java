package de.freese.arser.core.repository;

import java.net.URI;
import java.util.Objects;

import de.freese.arser.core.component.AbstractComponent;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepository extends AbstractComponent implements Repository {
    private final String name;
    private final URI uri;

    protected AbstractRepository(final URI uri, final String name) {
        super();

        this.uri = Objects.requireNonNull(uri, "uri required").normalize();
        this.name = Objects.requireNonNull(name, "name required");
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
