// Created: 19 Dez. 2024
package de.freese.arser.core.api;

import java.io.InputStream;
import java.io.OutputStream;

import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public interface RepositoryTest {
    boolean contains(ResourceRequest resourceRequest);

    InputStream getInputStream(ResourceRequest resourceRequest);

    void streamTo(ResourceRequest resourceRequest, OutputStream outputStream);

    void write(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception;
}
