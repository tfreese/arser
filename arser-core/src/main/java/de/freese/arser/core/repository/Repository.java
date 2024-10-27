// Created: 19.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.Lifecycle;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceInfo;
import de.freese.arser.core.response.ResourceResponse;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    ResourceInfo consume(ResourceRequest request, OutputStream outputStream) throws Exception;

    boolean exist(ResourceRequest request) throws Exception;

    ResourceResponse getInputStream(ResourceRequest request) throws Exception;

    /**
     * The name is the context-root.
     */
    String getName();

    URI getUri();

    default boolean isVirtual() {
        return false;
    }

    default boolean isWriteable() {
        return false;
    }

    void write(ResourceRequest request, InputStream inputStream) throws Exception;
}
