// Created: 03.05.2021
package de.freese.arser.core.repository.cached;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.BlobStore;
import de.freese.arser.core.request.ResourceResponse;

/**
 * @author Thomas Freese
 */
public class CachedResourceResponse extends ResourceResponse {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final BlobId blobId;
    private final BlobStore blobStore;

    public CachedResourceResponse(final ResourceResponse resourceResponse, final BlobId blobId, final BlobStore blobStore) {
        super(resourceResponse.getResourceRequest(), resourceResponse.getContentLength(), resourceResponse.getInputStream());

        this.blobId = Objects.requireNonNull(blobId, "BlobId required");
        this.blobStore = Objects.requireNonNull(blobStore, "BlobStore required");
    }

    @Override
    public long transferTo(final OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        long transferred = 0L;

        try (InputStream inputStream = new BufferedInputStream(getInputStream());
             OutputStream blobOutputStream = new BufferedOutputStream(blobStore.create(blobId))) {
            while ((read = inputStream.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
                blobOutputStream.write(buffer, 0, read);
                outputStream.write(buffer, 0, read);

                transferred += read;
            }

            blobOutputStream.flush();
        }
        catch (Exception ex) {
            if (ex instanceof IOException ioex) {
                throw ioex;
            }

            throw new IOException(ex);
        }

        return transferred;
    }
}
