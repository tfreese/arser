package de.freese.arser.blobvalue;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Thomas Freese
 */
public class EmptyBlobValue implements BlobValue {
    @Override
    public void close() throws IOException {
        // Empty
    }

    @Override
    public InputStream createInputStream() {
        return InputStream.nullInputStream();
    }

    @Override
    public long getContentLength() throws Exception {
        return -1L;
    }
}
