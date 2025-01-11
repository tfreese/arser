// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResponseHandler;

/**
 * @author Thomas Freese
 */
public class FileRepository extends AbstractRepository {
    private final boolean writeable;

    public FileRepository(final String contextRoot, final URI baseUri, final boolean writeable) {
        super(contextRoot, baseUri);

        this.writeable = writeable;
    }

    @Override
    protected boolean doExist(final ResourceRequest request) {
        final Path path = toPath(request.getResource());

        final boolean exist = Files.exists(path);

        if (getLogger().isDebugEnabled()) {
            if (exist) {
                getLogger().debug("exist - found: {}", path);
            }
            else {
                getLogger().debug("exist - not found: {}", path);
            }
        }

        return exist;
    }

    @Override
    protected void doStart() throws Exception {
        final Path path = Paths.get(getBaseUri());

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

    @Override
    protected void doStreamTo(final ResourceRequest request, final ResponseHandler handler) throws Exception {
        final Path path = toPath(request.getResource());

        if (Files.exists(path)) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Resource - found: {}", path);
            }

            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
                handler.onSuccess(Files.size(path), inputStream);
            }

            return;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - not found: {}", path);
        }

        handler.onError(new FileNotFoundException(path.toString()));
    }

    @Override
    protected void doWrite(final ResourceRequest request, final InputStream inputStream) throws Exception {
        if (!writeable) {
            throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getBaseUri());
        }

        final Path path = toPath(request.getResource());

        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
            inputStream.transferTo(outputStream);

            outputStream.flush();
        }
    }

    protected Path toPath(final URI resource) {
        final Path relativePath = toRelativePath(resource);

        return Paths.get(getBaseUri()).resolve(relativePath);
    }

    protected Path toRelativePath(final URI resource) {
        String uriPath = resource.getPath();
        uriPath = uriPath.replace(' ', '_');

        if (uriPath.startsWith("/")) {
            uriPath = uriPath.substring(1);
        }

        return Paths.get(uriPath);
    }
}
