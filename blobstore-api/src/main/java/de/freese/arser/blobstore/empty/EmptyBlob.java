// Created: 14.01.24
package de.freese.arser.blobstore.empty;

import java.io.InputStream;

import de.freese.arser.blobstore.api.AbstractBlob;
import de.freese.arser.blobstore.api.BlobId;

/**
 * @author Thomas Freese
 */
class EmptyBlob extends AbstractBlob {

    EmptyBlob(final BlobId id) {
        super(id);
    }

    @Override
    public InputStream createInputStream() throws Exception {
        return InputStream.nullInputStream();
    }

    @Override
    public long getLength() throws Exception {
        return -1;
    }
}
