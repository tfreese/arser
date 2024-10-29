// Created: 17.01.24
package de.freese.arser.core.response;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public interface ResourceResponse extends AutoCloseable {
    @Override
    void close();

    InputStream createInputStream() throws Exception;

    long getContentLength();

    default void transferTo(final OutputStream outputStream) throws Exception {
        try (InputStream is = new BufferedInputStream(createInputStream(), ArserUtils.DEFAULT_BUFFER_SIZE)) {
            is.transferTo(outputStream);
        }
    }
}
