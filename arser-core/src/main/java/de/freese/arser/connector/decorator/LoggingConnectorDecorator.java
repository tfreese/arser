package de.freese.arser.connector.decorator;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
public final class LoggingConnectorDecorator extends AbstractConnectorDecorator {

    public LoggingConnectorDecorator(final Connector delegate) {
        super(delegate);
    }

    @Override
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        getLogger().debug("execute: {}", request);

        return super.execute(request);
    }

    @Override
    public void start() throws Exception {
        getLogger().debug("starting connector: {}", getLogger().getName());

        super.start();
    }

    @Override
    public void stop() throws Exception {
        getLogger().debug("stopping connector: {}", getLogger().getName());

        super.stop();
    }
}
