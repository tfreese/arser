package de.freese.arser.indexer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.freese.arser.api.ArserRequest;

/**
 * @author Thomas Freese
 * @since 01.11.2024
 */
public final class ArtifactIndexerMemory implements ArtifactIndexer {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public String findRepository(final ArserRequest request) {
        return cache.getOrDefault(request.getId(), null);
    }

    @Override
    public void storeRepository(final ArserRequest request, final String repository) {
        cache.put(request.getId(), repository);
    }
}
