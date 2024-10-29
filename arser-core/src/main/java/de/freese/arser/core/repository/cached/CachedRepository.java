// Created: 03.05.2021
package de.freese.arser.core.repository.cached;

import java.net.URI;

import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.repository.AbstractRepository;
import de.freese.arser.core.repository.Repository;
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
        super(delegate.getName(), URI.create("cached"));

        this.delegate = assertNotNull(delegate, () -> "Repository");
        this.blobStore = assertNotNull(blobStore, () -> "BlobStore");
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

        if (resource.getPath().endsWith("maven-metadata.xml")) {
            // Never save these files, versions:display-dependency-updates won't work !
            return this.delegate.getResource(request);
        }

        final BlobId blobId = new BlobId(resource);

        if (getBlobStore().exists(blobId)) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Resource - found: {}", resource);
            }

            final Blob blob = getBlobStore().get(blobId);

            return new DefaultResourceResponse(blob.getLength(), blob::createInputStream);
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - not found: {}", resource);
        }

        final ResourceResponse response = this.delegate.getResource(request);

        if (response != null) {
            return new CachingResourceResponse(response, blobId, getBlobStore());
        }

        return null;
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
