package de.freese.arser.indexer;

import de.freese.arser.api.ArserRequest;

/**
 * @author Thomas Freese
 * @since 16.01.24
 */
public interface ArtifactIndexer {
    String findRepository(ArserRequest request);

    void storeRepository(ArserRequest request, String repository);
}
