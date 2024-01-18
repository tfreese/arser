// Created: 23.07.23
package de.freese.arser.core.repository.blobstore;

import java.io.InputStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.repository.AbstractRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.request.ResourceResponse;

/**
 * @author Thomas Freese
 */
public class BlobStoreRepository extends AbstractRepository {
    private static final Pattern PATTERN_SNAPSHOT_TIMESTAMP = Pattern.compile("\\d{8}\\.\\d{6}-\\d{1,}");

    private final BlobStore blobStore;

    public BlobStoreRepository(final String name, final URI uri, final BlobStore blobStore) {
        super(name, uri);

        this.blobStore = checkNotNull(blobStore, "BlobStore");
    }

    @Override
    public boolean isWriteable() {
        return true;
    }

    @Override
    protected boolean doExist(final ResourceRequest resourceRequest) throws Exception {
        final URI resource = resourceRequest.getResource();
        final BlobId blobId = new BlobId(removeSnapshotTimestamp(resource));

        final boolean exist = getBlobStore().exists(blobId);

        if (getLogger().isDebugEnabled()) {
            if (exist) {
                getLogger().debug("exist - found: {}", resource);
            }
            else {
                getLogger().debug("exist - not found: {}", resource);
            }
        }

        return exist;
    }

    @Override
    protected ResourceResponse doGetInputStream(final ResourceRequest resourceRequest) throws Exception {
        final URI resource = resourceRequest.getResource();
        final BlobId blobId = new BlobId(removeSnapshotTimestamp(resource));

        if (getBlobStore().exists(blobId)) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("getInputStream - found: {}", resource);
            }

            final Blob blob = getBlobStore().get(blobId);

            return new ResourceResponse(resourceRequest, blob.getLength(), blob.getInputStream());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - not found: {}", resource);
        }

        return null;
    }

    @Override
    protected void doWrite(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        final URI resource = resourceRequest.getResource();
        final BlobId blobId = new BlobId(removeSnapshotTimestamp(resource));

        getBlobStore().create(blobId, inputStream);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("written: {}", resource);
        }
    }

    protected BlobStore getBlobStore() {
        return blobStore;
    }

    protected URI removeSnapshotTimestamp(final URI uri) {
        if (!uri.getPath().contains("SNAPSHOT")) {
            return uri;
        }

        String uriValue = uri.toString();
        final Matcher matcher = PATTERN_SNAPSHOT_TIMESTAMP.matcher(uriValue);

        if (!matcher.find()) {
            return uri;
        }

        uriValue = matcher.replaceFirst("SNAPSHOT");

        getLogger().info("rewrite {} -> {}", uri, uriValue);

        return URI.create(uriValue);
    }
}
