package de.freese.arser.blobvalue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Thomas Freese
 */
public interface BlobValue extends AutoCloseable {

    @Override
    void close() throws IOException;

    /**
     * <b>This Stream MUST be closed to avoid resource exhausting!</b>
     */
    InputStream createInputStream() throws Exception;

    /**
     * @return Blob length in Byte.
     */
    long getContentLength() throws Exception;

    default void transferTo(final OutputStream outputStream) throws Exception {
        try (InputStream inputStream = createInputStream()) {
            inputStream.transferTo(outputStream);
        }
    }
}
