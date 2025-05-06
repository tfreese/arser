// Created: 06 Mai 2025
package de.freese.arser.core.response;

/**
 * @author Thomas Freese
 */
public abstract class AbstractResourceResponse implements ResourceResponse {
    private final long contentLength;

    protected AbstractResourceResponse(final long contentLength) {
        super();

        this.contentLength = contentLength;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }
}
