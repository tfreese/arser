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

    URI getBaseUri();

    String getContextRoot();

    /**
     * Schemes:
     * <ul>
     *     <li>file for local cached/stored resources</li>
     *     <li>http/https for remote resources</li>
     * </ul>
     */
    URI getDownloadUri(ResourceRequest request) throws Exception;

    ResourceResponse getResource(ResourceRequest request) throws Exception;

    default void write(final ResourceRequest request, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getBaseUri());
    }
}
