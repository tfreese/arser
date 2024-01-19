// Created: 19.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.Lifecycle;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.request.ResourceResponse;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    boolean exist(ResourceRequest resourceRequest) throws Exception;

    ResourceResponse getInputStream(ResourceRequest resourceRequest) throws Exception;

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

    void write(ResourceRequest resourceRequest, InputStream inputStream) throws Exception;
}
