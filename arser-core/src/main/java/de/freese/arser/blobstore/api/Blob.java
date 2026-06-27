// Created: 18.09.2019
package de.freese.arser.blobstore.api;

import de.freese.arser.blobvalue.BlobValue;

/**
 * Reference for binary Data from a {@link BlobStore}.<br>
 * <a href="https://github.com/sonatype/nexus-public/blob/master/components/nexus-blobstore-api">nexus-blobstore-api</a><br>
 * <a href="https://github.com/sonatype/goodies/tree/main/lifecycle">goodies</a>
 *
 * @author Thomas Freese
 */
public interface Blob {
    BlobValue getBlobValue();

    BlobId getId() throws Exception;
}
