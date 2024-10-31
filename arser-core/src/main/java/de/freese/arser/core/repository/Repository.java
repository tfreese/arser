// Created: 19.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.Lifecycle;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    boolean exist(ResourceRequest request) throws Exception;

    /**
     * The name is the context-root.
     */
    String getName();

    ResourceResponse getResource(ResourceRequest request) throws Exception;

    URI getUri();

    default void write(final ResourceRequest request, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getName() + " - " + getUri());
    }
}
