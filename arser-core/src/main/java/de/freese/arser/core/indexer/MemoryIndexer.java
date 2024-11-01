// Created: 01 Nov. 2024
package de.freese.arser.core.indexer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public final class MemoryIndexer implements ArtifactIndexer {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public String findRepository(final ResourceRequest request) {
        return cache.getOrDefault(request.getId(), null);
    }

    @Override
    public void storeRepository(final ResourceRequest request, final String repository) {
        cache.put(request.getId(), repository);
    }
}
