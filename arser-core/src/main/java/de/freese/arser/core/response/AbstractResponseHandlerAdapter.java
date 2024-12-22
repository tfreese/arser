// Created: 20 Dez. 2024
package de.freese.arser.core.response;

import java.io.InputStream;

/**
 * @author Thomas Freese
 */
public abstract class AbstractResponseHandlerAdapter implements ResponseHandler {
    @Override
    public void onError(final Exception exception) {
        // Empty
    }

    @Override
    public void onSuccess(final long contentLength, final InputStream inputStream) {
        // Empty
    }
}
