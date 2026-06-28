package de.freese.arser.repository;

import java.net.URI;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepository implements Repository {
    private final Logger logger = LoggerFactory.getLogger(getClass());
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

    protected Logger getLogger() {
        return logger;
    }
}
