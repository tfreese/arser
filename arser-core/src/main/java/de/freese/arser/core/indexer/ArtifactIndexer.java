// Created: 16.01.24
package de.freese.arser.core.indexer;

import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public interface ArtifactIndexer {
    String findRepository(ResourceRequest request);

    void storeRepository(ResourceRequest request, String repository);
}
