// Created: 19.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.Lifecycle;
import de.freese.arser.core.utils.HttpMethod;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    boolean exist(URI resource) throws Exception;

    RepositoryResponse getInputStream(URI resource) throws Exception;

    /**
     * The name is the context-root.
     */
    String getName();

    boolean supports(HttpMethod httpMethod);

    void write(URI resource, InputStream inputStream) throws Exception;
}
