// Created: 16.01.24
package de.freese.arser.core.indexer;

import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public interface ArtifactIndexer {
    Repository findRepository(ResourceRequest request);

    void storeRepository(ResourceRequest request, Repository repository);
}
