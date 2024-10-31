// Created: 03.05.2021
package de.freese.arser.core.repository;

import java.net.URI;
import java.util.Objects;

import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.CachingResourceResponse;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceResponse;

/**
 * @author Thomas Freese
 */
public class CachedRepository extends AbstractRepository {
    private final BlobStore blobStore;
    private final Repository delegate;

    public CachedRepository(final Repository delegate, final BlobStore blobStore) {
        super(delegate.getName(), URI.create(delegate.getName() + "cached"));

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

        return this.delegate.exist(request);
    }

    @Override
    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        final URI resource = request.getResource();

        ResourceResponse resourceResponse = null;

        if (resource.getPath().endsWith("maven-metadata.xml")) {
            // Never save these files, versions:display-dependency-updates won't work!
            resourceResponse = this.delegate.getResource(request);
        }
        else {
            final BlobId blobId = new BlobId(resource);

            if (getBlobStore().exists(blobId)) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Resource - found: {}", resource);
                }

                final Blob blob = getBlobStore().get(blobId);

                resourceResponse = new DefaultResourceResponse(blob.getLength(), blob::createInputStream);
            }
            else {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Resource - not found: {}", resource);
                }

                final ResourceResponse response = this.delegate.getResource(request);

                if (response != null) {
                    resourceResponse = new CachingResourceResponse(response, blobId, getBlobStore());
                }
            }
        }

        return resourceResponse;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        delegate.start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        delegate.stop();
    }

    protected BlobStore getBlobStore() {
        return blobStore;
    }
}
