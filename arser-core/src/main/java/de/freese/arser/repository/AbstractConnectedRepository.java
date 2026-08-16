package de.freese.arser.repository;

import java.net.URI;
import java.util.Objects;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.spi.BlockedException;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.connector.spi.NotFoundException;

/**
 * @author Thomas Freese
 */
@SuppressWarnings({"java:S5411"})
public abstract class AbstractConnectedRepository extends AbstractRepository {
    private final Connector connector;

    protected AbstractConnectedRepository(final AbstractRepositoryConfig config, final Connector connector) {
        super(config);

        this.connector = Objects.requireNonNull(connector, "connector required");
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        final ConnectorRequest<BlobValue> connectorRequest = ConnectorRequest.of(remoteUri, Operations.DOWNLOAD);

        try {
            final ConnectorResponse<BlobValue> connectorResponse = getConnector().execute(connectorRequest);

            return new ArserResult.Download(connectorResponse.value());
        }
        catch (final BlockedException ex) {
            return new ArserResult.Forbidden(remoteUri, ex.getMessage());
        }
        catch (NotFoundException _) {
            return new ArserResult.NotFound(remoteUri);
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        final ConnectorRequest<Boolean> connectorRequest = ConnectorRequest.of(remoteUri, Operations.EXISTS);

        try {
            final ConnectorResponse<Boolean> connectorResponse = getConnector().execute(connectorRequest);

            if (connectorResponse.value()) {
                return new ArserResult.Exist(remoteUri);
            }

            return new ArserResult.NotFound(remoteUri);
        }
        catch (final BlockedException ex) {
            return new ArserResult.Forbidden(remoteUri, ex.getMessage());
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }

    protected Connector getConnector() {
        return connector;
    }

    protected abstract URI toRemoteUri(URI uri, ArserRequest arserRequest);
}
