// Created: 14.01.24
package de.freese.arser.blobstore.empty;

import de.freese.arser.blobstore.api.AbstractBlob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.blobvalue.EmptyBlobValue;

/**
 * @author Thomas Freese
 */
class EmptyBlob extends AbstractBlob {
    private final BlobValue blobValue = new EmptyBlobValue();

    EmptyBlob(final BlobId id) {
        super(id);
    }

    @Override
    public BlobValue getBlobValue() {
        return blobValue;
    }
}
