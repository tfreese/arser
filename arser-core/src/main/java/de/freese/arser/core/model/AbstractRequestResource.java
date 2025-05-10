// Created: 06 Mai 2025
package de.freese.arser.core.model;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRequestResource implements RequestResource {
    private final long contentLength;

    protected AbstractRequestResource(final long contentLength) {
        super();

        this.contentLength = contentLength;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }
}
