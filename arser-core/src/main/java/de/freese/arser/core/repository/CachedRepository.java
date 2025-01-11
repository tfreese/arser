// Created: 03.05.2021
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResponseHandler;

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
    protected void doStart() throws Exception {
        delegate.start();
    }

    @Override
    protected void doStop() throws Exception {
        delegate.stop();
    }

    @Override
    protected void doStreamTo(final ResourceRequest request, final ResponseHandler handler) throws Exception {
        final URI remoteUri = request.getResource();

        if (remoteUri.getPath().endsWith("maven-metadata.xml")) {
            // Never save these files, versions:display-dependency-updates won't work!
            delegate.streamTo(request, handler);
        }
        else {
            final BlobId blobId = new BlobId(remoteUri);

            if (getBlobStore().exists(blobId)) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Resource - found: {}", request);
                }

                final Blob blob = getBlobStore().get(blobId);

                try (InputStream inputStream = blob.createBufferedInputStream()) {
                    handler.onSuccess(blob.getLength(), inputStream);
                }
            }
            else {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Resource - not found: {}", request);
                }

                delegate.streamTo(request, handler);
            }
        }
    }

    protected BlobStore getBlobStore() {
        return blobStore;
    }
}
