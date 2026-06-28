// Created: 16.01.24
package de.freese.arser.indexer;

import de.freese.arser.model.ArserRequest;

/**
 * @author Thomas Freese
 */
public interface ArtifactIndexer {
    String findRepository(ArserRequest request);

    void storeRepository(ArserRequest request, String repository);
}
