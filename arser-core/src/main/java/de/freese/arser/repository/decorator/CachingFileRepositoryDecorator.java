package de.freese.arser.repository.decorator;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.blobvalue.FileBlobValue;
import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 */
public final class CachingFileRepositoryDecorator extends AbstractRepositoryDecorator {

    private final Path cachePath;

    public CachingFileRepositoryDecorator(final Repository delegate, final Path cachePath) {
        super(delegate);

        this.cachePath = Objects.requireNonNull(cachePath, "cachePath required");
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final Path path = toAbsolutePath(cachePath, arserRequest);

        if (Files.exists(path)) {
            return new ArserResult.Download(new FileBlobValue(path));
        }

        final ArserResult result = super.download(arserRequest);

        if (result instanceof ArserResult.Download(final BlobValue blobValue)) {
            try {
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }

                try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    blobValue.transferTo(outputStream);
                    outputStream.flush();
                }
            }
            catch (final Exception ex) {
                return new ArserResult.Failure(ex);
            }
        }

        return result;
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final Path path = toAbsolutePath(cachePath, arserRequest);

        if (Files.exists(path)) {
            return new ArserResult.Exist(path.toUri());
        }

        return super.exist(arserRequest);
    }

    @Override
    public void start() throws Exception {
        super.start();

        if (!Files.exists(cachePath)) {
            Files.createDirectories(cachePath);
        }

        if (!Files.isReadable(cachePath)) {
            throw new IllegalStateException("path not readable: " + cachePath);
        }
    }

    private Path toAbsolutePath(final Path path, final ArserRequest arserRequest) {
        final String requestPath = arserRequest.getResource().getPath();

        return path.resolve(requestPath);
    }
}
