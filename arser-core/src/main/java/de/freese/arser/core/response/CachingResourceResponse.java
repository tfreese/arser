// Created: 03.05.2021
package de.freese.arser.core.response;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class CachingResourceResponse implements ResourceResponse {

    private final BlobId blobId;
    private final BlobStore blobStore;
    private final ResourceResponse delegate;

    public CachingResourceResponse(final ResourceResponse delegate, final BlobId blobId, final BlobStore blobStore) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
        this.blobId = Objects.requireNonNull(blobId, "BlobId required");
        this.blobStore = Objects.requireNonNull(blobStore, "BlobStore required");
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public InputStream createInputStream() throws Exception {
        return delegate.createInputStream();
    }

    @Override
    public long getContentLength() {
        return delegate.getContentLength();
    }

    @Override
    public void transferTo(final OutputStream outputStream) throws IOException {
        try {
            blobStore.create(blobId, blobOutputStream -> {
                try (InputStream inputStream = new BufferedInputStream(createInputStream(), ArserUtils.DEFAULT_BUFFER_SIZE)) {
                    ArserUtils.transferTo(inputStream, List.of(outputStream, blobOutputStream));
                }
            });
        }
        catch (IOException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IOException(ex);
        }
    }
}
