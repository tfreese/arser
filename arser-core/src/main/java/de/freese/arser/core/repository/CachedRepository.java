// Created: 03.05.2021
package de.freese.arser.core.repository;

import java.net.URI;
import java.util.Objects;

import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.model.RequestResource;
import de.freese.arser.core.model.ResourceRequest;

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
    protected boolean doExist(final ResourceRequest resourceRequest) throws Exception {
        final URI resource = resourceRequest.getResource();
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

        return delegate.exist(resourceRequest);
    }

    @Override
    protected RequestResource doGetResource(final ResourceRequest resourceRequest) throws Exception {
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
