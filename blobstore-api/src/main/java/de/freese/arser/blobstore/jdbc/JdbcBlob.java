package de.freese.arser.blobstore.jdbc;

import java.io.InputStream;
import java.util.Objects;

import de.freese.arser.blobstore.api.AbstractBlob;
import de.freese.arser.blobstore.api.BlobId;

/**
 * @author Thomas Freese
 */
class JdbcBlob extends AbstractBlob {
    private final JdbcBlobStore blobStore;

    private long length = Long.MAX_VALUE;

    JdbcBlob(final BlobId id, final JdbcBlobStore blobStore) {
        super(id);

        this.blobStore = Objects.requireNonNull(blobStore, "blobStore required");
    }

    @Override
    public InputStream createInputStream() throws Exception {
        return blobStore.inputStream(getId());
    }

    @Override
    public long getLength() throws Exception {
        if (length == Long.MAX_VALUE) {
            length = blobStore.length(getId());
        }

        return length;
    }
}
