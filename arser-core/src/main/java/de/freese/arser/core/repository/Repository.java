// Created: 19.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.Lifecycle;
import de.freese.arser.core.model.RequestResource;
import de.freese.arser.core.model.ResourceRequest;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    boolean exist(ResourceRequest resourceRequest) throws Exception;

    URI getBaseUri();

    String getContextRoot();

    RequestResource getResource(ResourceRequest resourceRequest) throws Exception;

    default void write(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getBaseUri());
    }
}
