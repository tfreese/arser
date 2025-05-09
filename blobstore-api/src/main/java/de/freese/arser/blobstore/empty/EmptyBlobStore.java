// Created: 14.01.24
package de.freese.arser.blobstore.empty;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import de.freese.arser.blobstore.api.AbstractBlobStore;
import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.ThrowingConsumer;

/**
 * @author Thomas Freese
 */
public class EmptyBlobStore extends AbstractBlobStore {
    private static final Blob NOOP_BLOB = new EmptyBlob(new BlobId(URI.create("empty")));
    private static final URI NOOP_URI = URI.create("empty");

    @Override
    public Blob create(final BlobId id, final ThrowingConsumer<OutputStream, Exception> consumer) throws Exception {
        consumer.accept(OutputStream.nullOutputStream());

        return get(id);
    }

    @Override
    public Blob create(final BlobId id, final InputStream inputStream) throws Exception {
        return get(id);
    }

    @Override
    public void delete(final BlobId id) throws Exception {
        // Empty
    }

    @Override
    public boolean exists(final BlobId id) throws Exception {
        return false;
    }

    @Override
    public Blob get(final BlobId id) throws Exception {
        return NOOP_BLOB;
    }

    @Override
    public URI getUri() {
        return NOOP_URI;
    }
}
