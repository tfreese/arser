// Created: 03.05.2021
package de.freese.arser.core.repository.cached;

import java.net.URI;

import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.repository.AbstractRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.CachedResourceResponse;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class CachedRepository extends AbstractRepository {
    private final BlobStore blobStore;
    private final Repository delegate;

    public CachedRepository(final Repository delegate, final BlobStore blobStore) {
        super(delegate.getName(), URI.create("cached"));

        this.delegate = checkNotNull(delegate, "Repository");
        this.blobStore = checkNotNull(blobStore, "BlobStore");
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
    protected ResourceResponse doGetInputStream(final ResourceRequest request) throws Exception {
        final URI resource = request.getResource();

        if (resource.getPath().endsWith("maven-metadata.xml")) {
            // Never save these files, versions:display-dependency-updates won't work !
            return this.delegate.getInputStream(request);
        }

        final BlobId blobId = new BlobId(resource);

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

        final ResourceResponse response = this.delegate.getInputStream(request);

        if (response != null) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Download {} Bytes [{}]: {} ", response.getContentLength(), ArserUtils.toHumanReadable(response.getContentLength()),
                        response.getResourceRequest().getResource());
            }

            return new CachedResourceResponse(response, blobId, getBlobStore());
        }

        return null;
    }

    protected BlobStore getBlobStore() {
        return blobStore;
    }
}
