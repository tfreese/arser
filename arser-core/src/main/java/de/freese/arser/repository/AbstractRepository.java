package de.freese.arser.repository;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepository implements Repository {
    private final AbstractRepositoryConfig config;

    // @JsonIgnore
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected AbstractRepository(final AbstractRepositoryConfig config) {
        super();

        this.config = Objects.requireNonNull(config, "config required");
    }

    @JsonValue
    @Override
    public AbstractRepositoryConfig getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return getConfig().toString();
    }

    protected Logger getLogger() {
        return logger;
    }
}
