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
public class CachedResourceResponse extends AbstractResourceResponse {

    private final BlobId blobId;
    private final BlobStore blobStore;

    public CachedResourceResponse(final ResourceResponse resourceResponse, final BlobId blobId, final BlobStore blobStore) {
        super(resourceResponse.getResourceRequest(), resourceResponse.getContentLength(), resourceResponse.getInputStream());

        this.blobId = Objects.requireNonNull(blobId, "BlobId required");
        this.blobStore = Objects.requireNonNull(blobStore, "BlobStore required");
    }

    @Override
    public void transferTo(final OutputStream outputStream) throws IOException {
        try {
            blobStore.create(blobId, blobOutputStream -> {
                try (InputStream inputStream = new BufferedInputStream(getInputStream(), ArserUtils.DEFAULT_BUFFER_SIZE)) {
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
