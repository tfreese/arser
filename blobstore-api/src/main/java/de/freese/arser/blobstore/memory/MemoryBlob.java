package de.freese.arser.blobstore.memory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import de.freese.arser.blobstore.api.AbstractBlob;
import de.freese.arser.blobstore.api.BlobId;

/**
 * @author Thomas Freese
 */
class MemoryBlob extends AbstractBlob {
    private final byte[] bytes;

    MemoryBlob(final BlobId id, final byte[] bytes) {
        super(id);

        this.bytes = bytes;
    }

    @Override
    public InputStream getInputStream() throws Exception {
        if (bytes == null) {
            return InputStream.nullInputStream();
        }

        return new ByteArrayInputStream(bytes);
    }

    @Override
    public long getLength() throws Exception {
        if (bytes == null) {
            return -1L;
        }

        return bytes.length;
    }
}
