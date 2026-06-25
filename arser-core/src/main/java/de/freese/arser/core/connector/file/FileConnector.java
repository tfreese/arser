package de.freese.arser.core.connector.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import de.freese.arser.core.connector.api.ConnectorRequest;
import de.freese.arser.core.connector.api.ConnectorResponse;
import de.freese.arser.core.connector.core.Attributes;
import de.freese.arser.core.connector.core.Operations;
import de.freese.arser.core.connector.security.UriGuard;
import de.freese.arser.core.connector.spi.Connector;
import de.freese.arser.core.connector.spi.ConnectorException;
import de.freese.arser.core.connector.spi.NotFoundException;
import de.freese.arser.core.connector.spi.UnsupportedOperationForSchemeException;

/**
 * @author Thomas Freese
 */
public final class FileConnector implements Connector {
    private final UriGuard uriGuard;

    public FileConnector() {
        this(UriGuard.ALLOW_ALL);
    }

    public FileConnector(final UriGuard uriGuard) {
        super();

        this.uriGuard = Objects.requireNonNull(uriGuard, "uriGuard required");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        if (!uriGuard.test(request.uri())) {
            throw new ConnectorException("Pfad blockiert: " + request.uri());
        }

        final Path path = Path.of(request.uri().normalize());

        try {
            return (ConnectorResponse<R>) switch (request.operation().name()) {
                case "exists" -> new ConnectorResponse<>(Files.isRegularFile(path), Map.of());
                case "download" -> {
                    if (!Files.exists(path)) {
                        throw new NotFoundException("Nicht gefunden: " + path);
                    }

                    yield new ConnectorResponse<>(Files.readAllBytes(path), Map.of("size", Files.size(path)));
                }
                case "download.stream" -> {
                    if (!Files.exists(path)) {
                        throw new NotFoundException("Nicht gefunden: " + path);
                    }

                    yield new ConnectorResponse<>(Files.newInputStream(path), Map.of("size", Files.size(path)));
                }
                case "upload" -> {
                    final byte[] body = request.attribute(Attributes.BODY).orElseThrow();

                    if (path.getParent() != null) {
                        Files.createDirectories(path.getParent());
                    }

                    Files.write(path, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                    yield new ConnectorResponse<>(null, Map.of("size", body.length));
                }
                case "delete" -> {
                    final boolean removed = Files.deleteIfExists(path);
                    yield new ConnectorResponse<>(null, Map.of("removed", removed));
                }
                case "list" -> {
                    try (Stream<Path> stream = Files.list(path)) {
                        final List<String> names = stream.map(Path::toString).toList();
                        yield new ConnectorResponse<>(names, Map.of("count", names.size()));
                    }
                }
                default -> throw new UnsupportedOperationForSchemeException(request.operation().name(), "file");
            };
        }
        catch (final IOException ex) {
            throw new ConnectorException("File-Exception:  " + path, ex);
        }
    }

    @Override
    public Set<String> supportedOperations() {
        return Set.of(Operations.EXISTS.name(),
                Operations.DOWNLOAD.name(),
                Operations.DOWNLOAD_STREAM.name(),
                Operations.UPLOAD.name(),
                Operations.DELETE.name(),
                Operations.LIST.name());
    }

    @Override
    public Set<String> supportedSchemes() {
        return Set.of("file");
    }
}
