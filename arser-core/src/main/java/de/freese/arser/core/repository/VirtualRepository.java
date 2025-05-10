// Created: 22.07.23
package de.freese.arser.core.repository;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.freese.arser.core.indexer.ArtifactIndexer;
import de.freese.arser.core.indexer.ArtifactIndexerMemory;
import de.freese.arser.core.model.FileResource;
import de.freese.arser.core.model.ResourceRequest;

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
    protected boolean doExist(final ResourceRequest resourceRequest) {
        if (artifactIndexer.findRepository(resourceRequest) != null) {
            return true;
        }

        boolean exist = false;

        for (final Repository repository : repositoryMap.values()) {
            try {
                exist = repository.exist(resourceRequest);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (exist) {
                artifactIndexer.storeRepository(resourceRequest, repository.getContextRoot());
                break;
            }
        }

        return exist;
    }

    @Override
    protected FileResource doGetResource(final ResourceRequest resourceRequest) throws Exception {
        final String repositoryContextRoot = artifactIndexer.findRepository(resourceRequest);
        final Repository repositoryIndexed = repositoryMap.get(repositoryContextRoot);

        if (repositoryIndexed != null) {
            return repositoryIndexed.getResource(resourceRequest);
        }

        for (final Repository repository : repositoryMap.values()) {
            FileResource fileResource = null;

            try {
                fileResource = repository.getResource(resourceRequest);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (fileResource != null) {
                return fileResource;
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
