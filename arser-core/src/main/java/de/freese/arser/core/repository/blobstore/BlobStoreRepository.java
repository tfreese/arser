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
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class BlobStoreRepository extends AbstractRepository {
    private static final Pattern PATTERN_SNAPSHOT_TIMESTAMP = ArserUtils.PATTERN_SNAPSHOT_TIMESTAMP;

    private final BlobStore blobStore;

    public BlobStoreRepository(final String name, final URI uri, final BlobStore blobStore) {
        super(name, uri);

        this.blobStore = assertNotNull(blobStore, () -> "BlobStore");
    }

    @Override
    public boolean isWriteable() {
        return true;
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI resource = request.getResource();
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
    protected ResourceResponse doGetInputStream(final ResourceRequest request) throws Exception {
        final URI resource = request.getResource();
        final BlobId blobId = new BlobId(removeSnapshotTimestamp(resource));

        if (getBlobStore().exists(blobId)) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("getInputStream - found: {}", resource);
            }

            final Blob blob = getBlobStore().get(blobId);

            return new DefaultResourceResponse(request, blob.getLength(), blob.getInputStream());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - not found: {}", resource);
        }

        return null;
    }

    @Override
    protected void doWrite(final ResourceRequest request, final InputStream inputStream) throws Exception {
        final URI resource = request.getResource();
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
