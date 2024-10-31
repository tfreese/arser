// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.freese.arser.core.config.LocalRepositoryConfig;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceResponse;

/**
 * @author Thomas Freese
 */
public class FileRepository extends AbstractRepository {
    private final boolean writeable;

    public FileRepository(final LocalRepositoryConfig config) {
        super(config.getName(), config.getUri());

        this.writeable = config.isWriteable();
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
    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        final Path path = toPath(request.getResource());

        if (Files.exists(path)) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Resource - found: {}", path);
            }

            return new DefaultResourceResponse(Files.size(path), () -> Files.newInputStream(path));
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - not found: {}", path);
        }

        return null;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final Path path = Paths.get(getUri());

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalStateException("path not readable: " + path);
        }
    }

    @Override
    protected void doWrite(final ResourceRequest request, final InputStream inputStream) throws Exception {
        if (!writeable) {
            throw new UnsupportedOperationException("read only repository: " + getName() + " - " + getUri());
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

        return Paths.get(getUri()).resolve(relativePath);
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
