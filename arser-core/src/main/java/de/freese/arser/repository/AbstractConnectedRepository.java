package de.freese.arser.repository;

import java.net.URI;
import java.util.Objects;

import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.model.ArserRequest;
import de.freese.arser.model.ArserResult;

/**
 * @author Thomas Freese
 */
public abstract class AbstractConnectedRepository extends AbstractRepository {
    private final Connector connector;

    public AbstractConnectedRepository(final URI uri, final String name, final Connector connector) {
        super(uri, name);

        this.connector = Objects.requireNonNull(connector, "connector required");
    }

    @Override
    public <R> ArserResult<R> download(final ArserRequest arserRequest) {
        final URI remoteUri = arserRequest.toRemoteUri(getUri());

        final ConnectorRequest<BlobValue> connectorRequest = ConnectorRequest.of(remoteUri, Operations.DOWNLOAD);

        try {
            final ConnectorResponse<BlobValue> connectorResponse = getConnector().execute(connectorRequest);

            return new ArserResult.Download<>(connectorResponse.value());
        }
        catch (final Exception ex) {
            return new ArserResult.Failure<>(ex);
        }
    }

    @Override
    public <R> ArserResult<R> exist(final ArserRequest arserRequest) {
        final URI remoteUri = arserRequest.toRemoteUri(getUri());

        final ConnectorRequest<Boolean> connectorRequest = ConnectorRequest.of(remoteUri, Operations.EXISTS);

        try {
            final ConnectorResponse<Boolean> connectorResponse = getConnector().execute(connectorRequest);

            if (connectorResponse.value()) {
                return new ArserResult.Exist<>(true);
            }

            return new ArserResult.NotFound<>(remoteUri);
        }
        catch (final Exception ex) {
            return new ArserResult.Failure<>(ex);
        }
    }

    protected Connector getConnector() {
        return connector;
    }
}
