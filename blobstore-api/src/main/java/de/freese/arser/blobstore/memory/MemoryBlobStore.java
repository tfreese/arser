package de.freese.arser.blobstore.memory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import de.freese.arser.blobstore.api.AbstractBlobStore;
import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.ThrowingConsumer;

/**
 * @author Thomas Freese
 */
public class MemoryBlobStore extends AbstractBlobStore {
    private static final URI MEMORY_URI = URI.create("memory");

    private final Map<BlobId, byte[]> cache = new HashMap<>();

    @Override
    public void create(final BlobId id, final ThrowingConsumer<OutputStream, Exception> consumer) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            consumer.accept(outputStream);

            outputStream.flush();

            cache.put(id, outputStream.toByteArray());
        }
    }

    @Override
    public void create(final BlobId id, final InputStream inputStream) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            inputStream.transferTo(baos);

            baos.flush();

            cache.put(id, baos.toByteArray());
        }
    }

    @Override
    public void delete(final BlobId id) throws Exception {
        cache.remove(id);
    }

    @Override
    public boolean exists(final BlobId id) throws Exception {
        return cache.containsKey(id);
    }

    @Override
    public URI getUri() {
        return MEMORY_URI;
    }

    @Override
    protected Blob doGet(final BlobId id) throws Exception {
        return new MemoryBlob(id, cache.get(id));
    }
}
