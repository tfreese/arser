// Created: 18.09.2019
package de.freese.arser.blobstore.api;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Reference for binary Data from a {@link BlobStore}.<br>
 * <a href="https://github.com/sonatype/nexus-public/blob/master/components/nexus-blobstore-api">nexus-blobstore-api</a><br>
 * <a href="https://github.com/sonatype/goodies/tree/main/lifecycle">goodies</a>
 *
 * @author Thomas Freese
 */
public interface Blob {
    /**
     * <b>This Stream MUST be closed to avoid resource exhausting!</b>
     */
    default InputStream createBufferedInputStream() throws Exception {
        return new BufferedInputStream(createInputStream());
    }

    /**
     * <b>This Stream MUST be closed to avoid resource exhausting!</b>
     */
    InputStream createInputStream() throws Exception;

    default byte[] getAllBytes() throws Exception {
        try (InputStream inputStream = createBufferedInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    BlobId getId() throws Exception;

    /**
     * Blob length in Byte.
     */
    long getLength() throws Exception;
}
