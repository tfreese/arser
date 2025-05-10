// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.freese.arser.core.model.DefaultRequestResource;
import de.freese.arser.core.model.RequestResource;
import de.freese.arser.core.model.ResourceRequest;

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
    protected boolean doExist(final ResourceRequest resourceRequest) {
        final Path path = toPath(resourceRequest.getResource());

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
    protected RequestResource doGetResource(final ResourceRequest resourceRequest) throws Exception {
        final Path path = toPath(resourceRequest.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("RequestResource: {}", path);
        }

        final long contentLength = Files.size(path);

        return new DefaultRequestResource(contentLength, () -> Files.newInputStream(path));
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
    protected void doWrite(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        if (!writeable) {
            throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getBaseUri());
        }

        final Path path = toPath(resourceRequest.getResource());

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

    protected Path toRelativePath(final URI uri) {
        String uriPath = uri.getPath();
        uriPath = uriPath.replace(' ', '_');

        if (uriPath.startsWith("/")) {
            uriPath = uriPath.substring(1);
        }

        return Paths.get(uriPath);
    }
}
