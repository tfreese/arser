// Created: 16.01.24
package de.freese.arser.core.indexer;

import de.freese.arser.core.model.ArserRequest;

/**
 * @author Thomas Freese
 */
public interface ArtifactIndexer {
    String findRepository(ArserRequest request);

    void storeRepository(ArserRequest request, String repository);
}
