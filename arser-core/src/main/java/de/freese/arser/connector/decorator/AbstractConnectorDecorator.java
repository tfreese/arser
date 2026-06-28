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

    protected AbstractConnectorDecorator(final Connector delegate) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
    }

    @Override
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> req) {
        return getDelegate().execute(req);
    }

    @Override
    public <R> CompletableFuture<ConnectorResponse<R>> executeAsync(final ConnectorRequest<R> req) {
        return getDelegate().executeAsync(req);
    }

    public Connector getDelegate() {
        return delegate;
    }

    @Override
    public void start() throws Exception {
        getDelegate().start();
    }

    @Override
    public void stop() throws Exception {
        getDelegate().stop();
    }

    @Override
    public Set<Operation<?>> supportedOperations() {
        return getDelegate().supportedOperations();
    }

    @Override
    public Set<String> supportedSchemes() {
        return getDelegate().supportedSchemes();
    }

    protected Logger getLogger() {
        if (delegate instanceof final AbstractConnectorDecorator acd) {
            return acd.getLogger();
        }

        return LoggerFactory.getLogger(getDelegate().getClass());
    }
}
