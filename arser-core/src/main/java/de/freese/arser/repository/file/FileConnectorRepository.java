package de.freese.arser.repository.file;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Attributes;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.file.FileConnector;
import de.freese.arser.connector.spi.BlockedException;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.repository.AbstractConnectedRepository;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.decorator.LoggingRepositoryDecorator;

/**
 * @author Thomas Freese
 */
public final class FileConnectorRepository extends AbstractConnectedRepository {
    public static Repository of(final FileRepositoryConfig config, final LifeCycleRegistry lifeCycleRegistry) {
        Repository repository = new FileConnectorRepository(config, new FileConnector());

        if (config.logging()) {
            repository = new LoggingRepositoryDecorator(repository);
        }

        lifeCycleRegistry.register(repository);

        return repository;
    }

    private FileConnectorRepository(final FileRepositoryConfig config, final Connector connector) {
        super(config, connector);
    }

    @Override
    public FileRepositoryConfig getConfig() {
        return (FileRepositoryConfig) super.getConfig();
    }

    @Override
    public void start() throws Exception {
        super.start();

        final Path path = Path.of(getUri());

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalStateException("path not readable: " + path);
        }
    }

    @Override
    public ArserResult upload(final ArserRequest arserRequest, final InputStream inputStream) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        if (isReadOnly()) {
            final String message = "repository is read only: %s [%s]".formatted(getName(), getClass().getSimpleName());

            return new ArserResult.Forbidden(remoteUri, message);
        }

        final ConnectorRequest<Long> connectorRequest = ConnectorRequest.of(remoteUri, Operations.UPLOAD_STREAM)
                .with(Attributes.BODY_STREAM, () -> inputStream);

        try {
            final ConnectorResponse<Long> connectorResponse = getConnector().execute(connectorRequest);

            // JreHttpClientConnector with UPLOAD_STREAM returns -1L!
            return new ArserResult.Upload(connectorResponse.value());
        }
        catch (final BlockedException ex) {
            return new ArserResult.Forbidden(remoteUri, ex.getMessage());
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }

    @Override
    protected URI toRemoteUri(final URI uri, final ArserRequest arserRequest) {
        final String uriPath = uri.getPath().replace(' ', '_');

        final String requestPath = arserRequest.getResource().getPath().replace(' ', '_');

        return Path.of(uriPath).resolve(requestPath).toUri();
    }

    private boolean isReadOnly() {
        return getConfig().readOnly();
    }
}
