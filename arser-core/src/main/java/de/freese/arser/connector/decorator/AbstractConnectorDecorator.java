package de.freese.arser.connector.decorator;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.api.Operation;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
public abstract class AbstractConnectorDecorator implements Connector {
    private final Connector delegate;
    private final Logger logger;

    protected AbstractConnectorDecorator(final Connector delegate) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");

        if (delegate instanceof final AbstractConnectorDecorator acd) {
            logger = acd.getLogger();
        } else {
            logger = LoggerFactory.getLogger(delegate.getClass());
        }
    }

    @Override
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> req) {
        return delegate.execute(req);
    }

    @Override
    public <R> CompletableFuture<ConnectorResponse<R>> executeAsync(final ConnectorRequest<R> req) {
        return delegate.executeAsync(req);
    }

    @Override
    public void start() throws Exception {
        delegate.start();
    }

    @Override
    public void stop() throws Exception {
        delegate.stop();
    }

    @Override
    public Set<Operation<?>> supportedOperations() {
        return delegate.supportedOperations();
    }

    @Override
    public Set<String> supportedSchemes() {
        return delegate.supportedSchemes();
    }

    protected Logger getLogger() {
        return logger;
    }
}
