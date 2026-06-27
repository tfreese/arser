// Created: 22.07.23
package de.freese.arser.core.repository.file;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import de.freese.arser.blobvalue.FileBlobValue;
import de.freese.arser.core.model.ArserRequest;
import de.freese.arser.core.model.ArserResult;
import de.freese.arser.core.repository.AbstractRepository;

/**
 * @author Thomas Freese
 */
public class FileRepository extends AbstractRepository {
    private final boolean writeable;

    public FileRepository(final URI uri, final String name, final boolean writeable) {
        super(uri, name);

        this.writeable = writeable;
    }

    @Override
    public <R> ArserResult<R> exist(final ArserRequest arserRequest) {
        final Path path = arserRequest.toLocalPath(getUri());

        final boolean exist = Files.exists(path);

        if (getLogger().isDebugEnabled()) {
            if (exist) {
                getLogger().debug("exist - found: {}", path);
            } else {
                getLogger().debug("exist - not found: {}", path);
            }
        }

        return new ArserResult.Exist<>(exist);
    }

    @Override
    public <R> ArserResult<R> getResource(final ArserRequest arserRequest) {
        final Path path = arserRequest.toLocalPath(getUri());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("RequestResource: {}", path);
        }

        return new ArserResult.Resource<>(new FileBlobValue(path));
    }

    @Override
    public <R> ArserResult<R> upload(final ArserRequest arserRequest, final InputStream inputStream) {
        if (!writeable) {
            return super.upload(arserRequest, inputStream);
        }

        final Path path = arserRequest.toLocalPath(getUri());

        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
                inputStream.transferTo(outputStream);

                outputStream.flush();
            }
        }
        catch (final IOException ex) {
            return new ArserResult.Failure<>(ex);
        }

        return new ArserResult.Uploaded<>();
    }

    @Override
    protected void doStart() throws Exception {
        final Path path = Path.of(getUri());

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalStateException("path not readable: " + path);
        }
    }

    @Override
    protected void doStop() throws Exception {
        // Empty
    }
}
