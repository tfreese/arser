// Created: 17.01.24
package de.freese.arser.core.request.handler;

import java.io.InputStream;

import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.request.ResourceResponse;

/**
 * @author Thomas Freese
 */
public interface RequestHandler {
    void addRepository(String contextRoot, Repository repository);

    boolean exist(ResourceRequest resourceRequest) throws Exception;

    ResourceResponse getInputStream(ResourceRequest resourceRequest) throws Exception;

    void write(ResourceRequest resourceRequest, InputStream inputStream) throws Exception;
}
