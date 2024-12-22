// Created: 20 Dez. 2024
package de.freese.arser.core.response;

import java.io.InputStream;

/**
 * @author Thomas Freese
 */
public interface ResponseHandler {
    void onError(Exception exception);

    void onSuccess(long contentLength, InputStream inputStream);
}
