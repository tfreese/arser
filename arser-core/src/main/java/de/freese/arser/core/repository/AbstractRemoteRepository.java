// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRemoteRepository extends AbstractRepository {
    /**
     * 1024L * 1024L = 1 MB
     */
    protected static final long KEEP_IN_MEMORY_LIMIT = 1_048_576L;
    // protected static final long KEEP_IN_MEMORY_LIMIT = 1024L;

    private final Path tempDir;

    protected AbstractRemoteRepository(final String name, final URI uri, final Path tempDir) {
        super(name, uri);

        this.tempDir = Objects.requireNonNull(tempDir, "tempDir required");
    }

    protected URI createRemoteUri(final URI uri, final URI resource) {
        String path = uri.getPath();
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

        return uri.resolve(path);
    }

    protected Path createTempFile() {
        final String fileName = UUID.randomUUID().toString();

        return tempDir.resolve(fileName);
    }

    @Override
    protected void doStart() throws Exception {
        final String scheme = getUri().getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            final String msg = "HTTP or HTTPS protocol required: " + scheme;

            getLogger().error(msg);

            throw new IllegalArgumentException(msg);
        }

        Files.createDirectories(tempDir);
    }

    @Override
    protected void doStop() throws Exception {
        // Empty
    }

    protected Path saveTemp(final InputStream inputStream) throws IOException {
        final Path tempFile = createTempFile();

        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        return tempFile;
    }
}
