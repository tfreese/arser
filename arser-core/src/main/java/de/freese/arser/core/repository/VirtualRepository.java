// Created: 22.07.23
package de.freese.arser.core.repository;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.freese.arser.core.indexer.ArtifactIndexer;
import de.freese.arser.core.indexer.ArtifactIndexerMemory;
import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public class VirtualRepository extends AbstractRepository {

    private final ArtifactIndexer artifactIndexer = new ArtifactIndexerMemory();
    private final Map<String, Repository> repositoryMap = new HashMap<>();

    public VirtualRepository(final String contextRoot, final List<Repository> repositories) {
        super(contextRoot, URI.create("virtual"));

        for (Repository repository : repositories) {
            if (repositoryMap.containsKey(repository.getContextRoot())) {
                throw new IllegalStateException("repository already exist: " + repository.getContextRoot());
            }

            repositoryMap.put(repository.getContextRoot(), repository);
        }
    }

    @Override
    protected boolean doExist(final ResourceRequest request) {
        if (artifactIndexer.findRepository(request) != null) {
            return true;
        }

        boolean exist = false;

        for (final Repository repository : repositoryMap.values()) {
            try {
                exist = repository.exist(request);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (exist) {
                artifactIndexer.storeRepository(request, repository.getContextRoot());
                break;
            }
        }

        return exist;
    }

    @Override
    protected URI doGetDownloadUri(final ResourceRequest request) throws Exception {
        final String repositoryContextRoot = artifactIndexer.findRepository(request);
        final Repository repositoryIndexed = repositoryMap.get(repositoryContextRoot);

        if (repositoryIndexed != null) {
            return repositoryIndexed.getDownloadUri(request);
        }

        for (final Repository repository : repositoryMap.values()) {
            URI uri = null;

            try {
                uri = repository.getDownloadUri(request);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (uri != null) {
                return uri;
            }
        }

        return null;
    }

    @Override
    protected void doStart() throws Exception {
        // Empty
    }

    @Override
    protected void doStop() throws Exception {
        // Empty
    }
}
