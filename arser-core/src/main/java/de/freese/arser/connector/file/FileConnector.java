package de.freese.arser.connector.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import de.freese.arser.blobvalue.FileBlobValue;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Attributes;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.security.UriGuard;
import de.freese.arser.connector.spi.AbstractConnector;
import de.freese.arser.connector.spi.ConnectorException;
import de.freese.arser.connector.spi.NotFoundException;
import de.freese.arser.connector.spi.UnsupportedOperationForSchemeException;

/**
 * @author Thomas Freese
 */
public final class FileConnector extends AbstractConnector {
    private final UriGuard uriGuard;

    public FileConnector() {
        this(UriGuard.ALLOW_ALL);
    }

    public FileConnector(final UriGuard uriGuard) {
        super(Set.of("file"), Set.of(
                Operations.DELETE,
                Operations.DOWNLOAD,
                Operations.EXISTS,
                Operations.LIST,
                Operations.UPLOAD));

        this.uriGuard = Objects.requireNonNull(uriGuard, "uriGuard required");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        if (!uriGuard.test(request.uri())) {
            throw new ConnectorException("Blocked by UriGuard: " + request.uri());
        }

        final Path path = Path.of(request.uri().normalize());

        try {
            final ConnectorResponse<?> response;

            if (Operations.DELETE.equals(request.operation())) {
                final boolean removed = Files.deleteIfExists(path);

                response = new ConnectorResponse<>(null, Map.of("removed", removed));
            } else if (Operations.DOWNLOAD.equals(request.operation())) {
                if (!Files.exists(path)) {
                    throw new NotFoundException("Nicht gefunden: " + path);
                }

                response = new ConnectorResponse<>(new FileBlobValue(path), Map.of("size", Files.size(path)));
            } else if (Operations.EXISTS.equals(request.operation())) {
                response = new ConnectorResponse<>(Files.isRegularFile(path), Map.of());
            } else if (Operations.LIST.equals(request.operation())) {
                try (Stream<Path> stream = Files.list(path)) {
                    final List<String> names = stream.map(Path::toString).toList();

                    response = new ConnectorResponse<>(names, Map.of("count", names.size()));
                }
            } else if (Operations.UPLOAD.equals(request.operation())) {
                final byte[] body = request.attribute(Attributes.BODY).orElseThrow();

                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }

                Files.write(path, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                response = new ConnectorResponse<>((long) body.length, Map.of());
            } else if (Operations.UPLOAD_STREAM.equals(request.operation())) {
                final Supplier<InputStream> supplier = request.attribute(Attributes.BODY_STREAM).orElseThrow();

                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }

                try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    supplier.get().transferTo(outputStream);
                    outputStream.flush();
                }

                response = new ConnectorResponse<>(Files.size(path), Map.of());
            } else {
                throw new UnsupportedOperationForSchemeException(request.operation().name(), request.uri().getScheme());
            }

            return (ConnectorResponse<R>) response;
        }
        catch (final IOException ex) {
            throw new ConnectorException("File-Exception:  " + path, ex);
        }
    }
}
