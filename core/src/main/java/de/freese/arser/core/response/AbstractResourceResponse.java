// Created: 17.01.24
package de.freese.arser.core.response;

import java.io.InputStream;
import java.util.Objects;

import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public abstract class AbstractResourceResponse implements ResourceResponse {
    private final long contentLength;
    private final InputStream inputStream;
    private final ResourceRequest resourceRequest;

    protected AbstractResourceResponse(final ResourceRequest resourceRequest, final long contentLength, final InputStream inputStream) {
        super();

        this.resourceRequest = Objects.requireNonNull(resourceRequest, "resourceRequest required");
        this.contentLength = contentLength;
        this.inputStream = Objects.requireNonNull(inputStream, "inputStream required");
    }

    @Override
    public long getContentLength() {
        return this.contentLength;
    }

    @Override
    public String getFileName() {
        final String path = getResourceRequest().getResource().getPath();
        final int lastSlashIndex = path.lastIndexOf('/');

        return path.substring(lastSlashIndex + 1);
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public ResourceRequest getResourceRequest() {
        return resourceRequest;
    }
}
