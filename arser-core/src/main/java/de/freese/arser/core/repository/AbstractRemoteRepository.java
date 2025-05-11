// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRemoteRepository extends AbstractRepository {
    private final Path workingDir;

    protected AbstractRemoteRepository(final String contextRoot, final URI baseUri, final Path workingDir) {
        super(contextRoot, baseUri);

        this.workingDir = Objects.requireNonNull(workingDir, "workingDir required");

        if (!Files.exists(workingDir)) {
            try {
                Files.createDirectories(workingDir);
            }
            catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    protected URI createRemoteUri(final URI baseUri, final URI resource) {
        String path = baseUri.getPath();
        final String pathResource = resource.getPath();

        if (path.endsWith("/") && pathResource.startsWith("/")) {
            path += pathResource.substring(1);
        }
        else if (path.endsWith("/") && !pathResource.startsWith("/")) {
            path += pathResource;
        }
        else if (!path.endsWith("/") && pathResource.startsWith("/")) {
            path += pathResource;
        }

        return baseUri.resolve(path);
    }

    @Override
    protected void doStart() throws Exception {
        final String scheme = getBaseUri().getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            final String msg = "HTTP or HTTPS protocol required: " + scheme;

            getLogger().error(msg);

            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    protected void doStop() throws Exception {
        // Empty
    }
}
