// Created: 24.07.23
package de.freese.arser.core.component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.blobstore.file.FileBlobStore;
import de.freese.arser.blobstore.jdbc.JdbcBlobStore;
import de.freese.arser.core.lifecycle.AbstractLifecycle;

/**
 * @author Thomas Freese
 */
public class BlobStoreComponent extends AbstractLifecycle {

    private final BlobStore blobStore;

    public BlobStoreComponent(final BlobStore blobStore) {
        super();

        this.blobStore = Objects.requireNonNull(blobStore, "blobStore required");
    }

    public BlobStore getBlobStore() {
        return blobStore;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [");
        sb.append("blobStore=").append(blobStore);
        sb.append(']');

        return sb.toString();
    }

    @Override
    protected void doStart() throws Exception {
        if (blobStore instanceof FileBlobStore) {
            final Path path = Paths.get(blobStore.getUri());

            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            if (!Files.isReadable(path)) {
                throw new IllegalStateException("path not readable: " + path);
            }

            if (!Files.isWritable(path)) {
                throw new IllegalStateException("path not writeable: " + path);
            }
        }
        else if (blobStore instanceof JdbcBlobStore jdbcBlobStore) {
            jdbcBlobStore.createDatabaseIfNotExist();
        }
    }

    @Override
    protected void doStop() throws Exception {
        // Empty
    }
}
