// Created: 16.01.24
package de.freese.arser.core.indexer;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public interface ArtifactIndexer {
    String findRepository(URI resource);

    void storeRepository(URI resource, String repository);
}
