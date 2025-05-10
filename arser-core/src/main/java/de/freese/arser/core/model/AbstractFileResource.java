// Created: 06 Mai 2025
package de.freese.arser.core.model;

/**
 * @author Thomas Freese
 */
public abstract class AbstractFileResource implements FileResource {
    private final long contentLength;

    protected AbstractFileResource(final long contentLength) {
        super();

        this.contentLength = contentLength;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }
}
