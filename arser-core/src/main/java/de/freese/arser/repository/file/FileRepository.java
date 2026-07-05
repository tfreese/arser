package de.freese.arser.repository.file;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.FileBlobValue;
import de.freese.arser.repository.AbstractRepository;

/**
 * @author Thomas Freese
 */
public final class FileRepository extends AbstractRepository {
    public static FileRepositoryBuilder builder() {
        return new FileRepositoryBuilder();
    }

    private final boolean readOnly;

    FileRepository(final URI uri, final String name, final boolean readOnly) {
        super(uri, name);

        this.readOnly = readOnly;
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final Path path = toAbsolutePath(getUri(), arserRequest);

        if (!Files.exists(path)) {
            return new ArserResult.NotFound(path.toUri());
        }

        return new ArserResult.Download(new FileBlobValue(path));
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final Path path = toAbsolutePath(getUri(), arserRequest);

        if (!Files.exists(path)) {
            return new ArserResult.NotFound(path.toUri());
        }

        return new ArserResult.Exist(path.toUri());
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
        final Path path = toAbsolutePath(getUri(), arserRequest);

        if (readOnly) {
            final String message = "repository is read only: %s [%s]".formatted(getName(), getClass().getSimpleName());

            return new ArserResult.Forbidden(path.toUri(), message);
        }

        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            }

            return new ArserResult.Upload(Files.size(path));
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }

    private Path toAbsolutePath(final URI uri, final ArserRequest arserRequest) {
        final String uriPath = uri.getPath();

        final String requestPath = arserRequest.getResource().getPath();

        return Path.of(uriPath).resolve(requestPath);
    }
}
