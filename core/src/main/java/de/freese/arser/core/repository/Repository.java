// Created: 19.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.Lifecycle;

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

    default boolean isVirtual() {
        return false;
    }

    default boolean isWriteable() {
        return false;
    }

    void write(URI resource, InputStream inputStream) throws Exception;
}
