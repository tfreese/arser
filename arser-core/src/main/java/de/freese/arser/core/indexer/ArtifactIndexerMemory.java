// Created: 01 Nov. 2024
package de.freese.arser.core.indexer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public final class ArtifactIndexerMemory implements ArtifactIndexer {
    private final Map<String, Repository> cache = new ConcurrentHashMap<>();

    @Override
    public Repository findRepository(final ResourceRequest request) {
        return cache.getOrDefault(request.getId(), null);
    }

    @Override
    public void storeRepository(final ResourceRequest request, final Repository repository) {
        cache.put(request.getId(), repository);
    }
}
