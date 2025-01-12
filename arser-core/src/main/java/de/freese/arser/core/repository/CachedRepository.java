// Created: 03.05.2021
package de.freese.arser.core.repository;

import java.net.URI;
import java.util.Objects;

import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public class CachedRepository extends AbstractRepository {
    private final BlobStore blobStore;
    private final Repository delegate;

    public CachedRepository(final Repository delegate, final BlobStore blobStore) {
        super(delegate.getContextRoot(), URI.create(delegate.getContextRoot() + "cached"));

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
        this.blobStore = Objects.requireNonNull(blobStore, "blobStore required");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI resource = request.getResource();
        final BlobId blobId = new BlobId(resource);

        final boolean exist = getBlobStore().exists(blobId);

        if (exist) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("exist - found: {}", resource);
            }

            return true;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - not found: {}", resource);
        }

        return delegate.exist(request);
    }

    @Override
    protected URI doGetDownloadUri(final ResourceRequest request) throws Exception {
        final URI remoteUri = request.getResource();

        if (remoteUri.getPath().endsWith("maven-metadata.xml")) {
            // Never save these files, versions:display-dependency-updates won't work!
            return delegate.getDownloadUri(request);
        }

        final BlobId blobId = new BlobId(remoteUri);

        if (getBlobStore().exists(blobId)) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Resource - found: {}", request);
            }

            // final Blob blob = getBlobStore().get(blobId);

            return blobId.getUri();
        }
        else {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Resource - not found: {}", request);
            }

            // final URI uri = delegate.getDownloadUri(request);

            // TODO Download and save
        }

        return null;
    }

    @Override
    protected void doStart() throws Exception {
        delegate.start();
    }

    @Override
    protected void doStop() throws Exception {
        delegate.stop();
    }

    protected BlobStore getBlobStore() {
        return blobStore;
    }
}
