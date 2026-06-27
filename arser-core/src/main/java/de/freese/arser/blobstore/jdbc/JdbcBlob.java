package de.freese.arser.blobstore.jdbc;

import java.util.Objects;

import de.freese.arser.blobstore.api.AbstractBlob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobvalue.BlobValue;

/**
 * @author Thomas Freese
 */
class JdbcBlob extends AbstractBlob {
    private final BlobValue blobValue;

    JdbcBlob(final BlobId id, final BlobValue blobValue) {
        super(id);

        this.blobValue = Objects.requireNonNull(blobValue, "blobValue required");
    }

    @Override
    public BlobValue getBlobValue() {
        return blobValue;
    }
}
