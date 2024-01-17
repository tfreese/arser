// Created: 17.01.24
package de.freese.arser.core.request;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import de.freese.arser.core.repository.RepositoryResponse;

/**
 * @author Thomas Freese
 */
public class ResourceResponse {

    private final long contentLength;
    private final InputStream inputStream;
    private final RepositoryResponse repositoryResponse;
    private final ResourceRequest resourceRequest;

    public ResourceResponse(final ResourceRequest resourceRequest, final long contentLength, final InputStream inputStream) {
        super();

        this.resourceRequest = Objects.requireNonNull(resourceRequest, "resourceRequest required");
        this.contentLength = contentLength;
        this.inputStream = Objects.requireNonNull(inputStream, "inputStream required");
        this.repositoryResponse = null;
    }

    public ResourceResponse(final ResourceRequest resourceRequest, final long contentLength, final RepositoryResponse repositoryResponse) {
        super();

        this.resourceRequest = Objects.requireNonNull(resourceRequest, "resourceRequest required");
        this.contentLength = contentLength;
        this.inputStream = null;
        this.repositoryResponse = Objects.requireNonNull(repositoryResponse, "repositoryResponse required");
    }

    public long getContentLength() {
        return this.contentLength;
    }

    public String getFileName() {
        final String path = getResourceRequest().getResource().getPath();
        final int lastSlashIndex = path.lastIndexOf('/');

        return path.substring(lastSlashIndex + 1);
    }

    public ResourceRequest getResourceRequest() {
        return resourceRequest;
    }

    public long transferTo(final OutputStream outputStream) throws IOException {
        if (repositoryResponse != null) {
            return repositoryResponse.transferTo(outputStream);
        }
        else if (inputStream instanceof BufferedInputStream) {
            try (InputStream is = inputStream) {
                return is.transferTo(outputStream);
            }
        }
        else {
            try (InputStream is = new BufferedInputStream(inputStream)) {
                return is.transferTo(outputStream);
            }
        }
    }
}
