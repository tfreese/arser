package de.freese.arser.connector.decorator;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
public final class LoggingConnector extends AbstractConnectorDecorator {

    public LoggingConnector(final Connector delegate) {
        super(delegate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        getLogger().debug("{} - execute: {}", getClass().getSimpleName(), request);

        return super.execute(request);
    }
}
