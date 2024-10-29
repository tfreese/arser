// Created: 17.01.24
package de.freese.arser.core.response;

import java.io.InputStream;
import java.util.Objects;

/**
 * @author Thomas Freese
 */
public class DefaultResourceResponse implements ResourceResponse {
    private final long contentLength;
    private final ResourceHandle resourceHandle;

    public DefaultResourceResponse(final long contentLength, final ResourceHandle resourceHandle) {
        super();

        this.contentLength = contentLength;
        this.resourceHandle = Objects.requireNonNull(resourceHandle, "resourceHandle required");
    }

    @Override
    public void close() {
        resourceHandle.close();
    }

    @Override
    public InputStream createInputStream() throws Exception {
        return resourceHandle.createInputStream();
    }

    @Override
    public long getContentLength() {
        return this.contentLength;
    }
}
