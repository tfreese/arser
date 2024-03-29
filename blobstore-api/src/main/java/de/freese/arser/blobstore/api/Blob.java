// Created: 18.09.2019
package de.freese.arser.blobstore.api;

import java.io.InputStream;

/**
 * Reference for binary Data from a {@link BlobStore}.<br>
 * <a href="https://github.com/sonatype/nexus-public/blob/master/components/nexus-blobstore-api">nexus-blobstore-api</a><br>
 * <a href="https://github.com/sonatype/goodies/tree/main/lifecycle">goodies</a>
 *
 * @author Thomas Freese
 */
public interface Blob {
    default byte[] getAllBytes() throws Exception {
        try (InputStream inputStream = getInputStream()) {
            return inputStream.readAllBytes();
        }

        //        byte[] bytes = null;
        //
        //        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //             InputStream inputStream = getInputStream())
        //        {
        //            inputStream.transferTo(baos);
        //
        //            baos.flush();
        //
        //            bytes = baos.toBytes();
        //        }
        //
        //        return bytes;
    }

    BlobId getId() throws Exception;

    /**
     * <b>This Stream MUST be closed to avoid resource exhausting !</b>
     */
    InputStream getInputStream() throws Exception;

    /**
     * Blob length in Byte.
     */
    long getLength() throws Exception;
}
