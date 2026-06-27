// Created: 18.09.2019
package de.freese.arser.blobstore.api;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

/**
 * Unique ID for a Blob from a {@link BlobStore}.<br>
 * <a href="https://github.com/sonatype/nexus-public/blob/master/components/nexus-blobstore-api">nexus-blobstore-api</a><br>
 * <a href="https://github.com/sonatype/goodies/tree/main/lifecycle">goodies</a>
 *
 * @author Thomas Freese
 */
public record BlobId(URI uri) implements Serializable, Comparable<BlobId> {
    @Serial
    private static final long serialVersionUID = -5581749917166864024L;

    public BlobId(final URI uri) {

        this.uri = Objects.requireNonNull(uri, "uri required");
    }

    @Override
    public int compareTo(final BlobId o) {
        return uri.compareTo(o.uri);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final BlobId blobId = (BlobId) obj;

        return Objects.equals(uri, blobId.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
