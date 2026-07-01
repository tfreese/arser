package de.freese.arser.connector.spi;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.connector.api.Operation;

/**
 * @author Thomas Freese
 */
public abstract class AbstractConnector implements Connector {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Set<Operation<?>> supportedOperations;
    private final Set<String> supportedSchemes;

    protected AbstractConnector(final Set<String> supportedSchemes, final Set<Operation<?>> supportedOperations) {
        super();

        this.supportedSchemes = Set.copyOf(Objects.requireNonNull(supportedSchemes, "supportedSchemes required"));
        this.supportedOperations = Set.copyOf(Objects.requireNonNull(supportedOperations, "supportedOperations required"));
    }

    @Override
    public Set<Operation<?>> supportedOperations() {
        return supportedOperations;
    }

    @Override
    public Set<String> supportedSchemes() {
        return supportedSchemes;
    }

    protected Logger getLogger() {
        return logger;
    }
}
