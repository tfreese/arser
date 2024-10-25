// Created: 03.05.2021
package de.freese.arser.core.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.utils.MultiplexOutputStream;

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
    public long transferTo(final OutputStream outputStream) throws IOException {
        final AtomicLong transferred = new AtomicLong(0L);

        try {
            blobStore.create(blobId, blobOutputStream -> {
                final MultiplexOutputStream multiplexOutputStream = new MultiplexOutputStream(List.of(outputStream, blobOutputStream));

                try (InputStream inputStream = getInputStream()) {
                    transferred.addAndGet(inputStream.transferTo(multiplexOutputStream));
                }
            });
        }
        catch (IOException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IOException(ex);
        }

        return transferred.get();
    }
}
