package de.freese.arser.repository.file;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Attributes;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.file.FileConnector;
import de.freese.arser.model.ArserRequest;
import de.freese.arser.model.ArserResult;
import de.freese.arser.repository.AbstractConnectedRepository;
import de.freese.arser.repository.RepositoryException;

/**
 * @author Thomas Freese
 */
public final class FileRepository extends AbstractConnectedRepository {
    public static FileRepositoryBuilder builder() {
        return new FileRepositoryBuilder();
    }

    private final boolean readOnly;

    FileRepository(final URI uri, final String name, final FileConnector connector, final boolean readOnly) {
        super(uri, name, connector);

        this.readOnly = readOnly;
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
    public <R> ArserResult<R> upload(final ArserRequest arserRequest, final InputStream inputStream) {
        if (readOnly) {
            final String message = "repository is read only: %s [%s]".formatted(getName(), getClass().getSimpleName());

            return new ArserResult.Failure<>(new RepositoryException(message));
        }

        final URI remoteUri = arserRequest.toRemoteUri(getUri());

        final ConnectorRequest<Long> connectorRequest = ConnectorRequest.of(remoteUri, Operations.UPLOAD_STREAM)
                .with(Attributes.BODY_STREAM, () -> inputStream);

        try {
            final ConnectorResponse<Long> connectorResponse = getConnector().execute(connectorRequest);

            // JreHttpClientConnector with UPLOAD_STREAM returns -1L!
            return new ArserResult.Upload<>(connectorResponse.value());
        }
        catch (final Exception ex) {
            return new ArserResult.Failure<>(ex);
        }
    }
}
