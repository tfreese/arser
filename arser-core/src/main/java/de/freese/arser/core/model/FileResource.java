// Created: 06 Mai 2025
package de.freese.arser.core.model;

import java.io.OutputStream;

/**
 * @author Thomas Freese
 */
public interface FileResource {
    long getContentLength();

    void transferTo(OutputStream outputStream) throws Exception;

    // default void transferTo(final OutputStream outputStream) throws Exception {
    //     try (InputStream is = new BufferedInputStream(createInputStream(), ArserUtils.DEFAULT_BUFFER_SIZE)) {
    //         is.transferTo(outputStream);
    //     }
    // }
}
