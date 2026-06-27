package de.freese.arser.blobstore.memory;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.freese.arser.blobstore.api.AbstractBlobStore;
import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.blobvalue.DefaultBlobValue;
import de.freese.arser.blobvalue.EmptyBlobValue;

/**
 * @author Thomas Freese
 */
public class MemoryBlobStore extends AbstractBlobStore {
    private static final URI MEMORY_URI = URI.create("memory");

    private final Map<BlobId, BlobValue> cache = new HashMap<>();

    @Override
    public Blob create(final BlobId id, final InputStream inputStream) throws Exception {
        final BlobValue blobValue = DefaultBlobValue.of(inputStream);

        cache.put(id, blobValue);

        return get(id);
    }

    @Override
    public void delete(final BlobId id) throws Exception {
        final BlobValue blobValue = cache.remove(id);

        if (blobValue != null) {
            blobValue.close();
        }
    }

    @Override
    public boolean exists(final BlobId id) throws Exception {
        return cache.containsKey(id);
    }

    @Override
    public Blob get(final BlobId id) throws Exception {
        final BlobValue blobValue = cache.get(id);

        return new MemoryBlob(id, Objects.requireNonNullElseGet(blobValue, EmptyBlobValue::new));

    }

    @Override
    public URI getUri() {
        return MEMORY_URI;
    }
}
