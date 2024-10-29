// Created: 29 Okt. 2024
package de.freese.arser.core.response;

import java.io.InputStream;

/**
 * @author Thomas Freese
 */
public interface ResourceHandle {
    default void close() {
        // Empty
    }

    InputStream createInputStream() throws Exception;
}
