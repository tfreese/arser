// Created: 19 Dez. 2024
package de.freese.arser.core.api;

import java.io.InputStream;

/**
 * @author Thomas Freese
 */
public interface ResourceResponseTest<R> {
    R handleResponse(int httpStatus, long contentLength, InputStream inputStream);

    // R onError(int httpStatus, Exception exception);
}
