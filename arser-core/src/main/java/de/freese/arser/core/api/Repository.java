// Created: 19.07.23
package de.freese.arser.core.api;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.Lifecycle;
import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    boolean exist(ResourceRequest request) throws Exception;

    String getContextRoot();

    URI getUri();

    void streamTo(ResourceRequest resourceRequest, ResponseHandler handler) throws Exception;

    default void write(final ResourceRequest request, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getUri());
    }
}
