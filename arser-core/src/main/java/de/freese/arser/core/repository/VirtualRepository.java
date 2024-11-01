// Created: 22.07.23
package de.freese.arser.core.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import de.freese.arser.core.config.ConfigValidator;
import de.freese.arser.core.config.VirtualRepositoryConfig;
import de.freese.arser.core.indexer.ArtifactIndexer;
import de.freese.arser.core.indexer.MemoryIndexer;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;

/**
 * @author Thomas Freese
 */
public class VirtualRepository extends AbstractRepository {

    private final ArtifactIndexer artifactIndexer = new MemoryIndexer();
    private final Map<String, Repository> repositoryMap = new HashMap<>();

    public VirtualRepository(final VirtualRepositoryConfig config) {
        super(config.getName(), config.getUri());

        ConfigValidator.value(config.getRepositories(), value -> value != null && !value.isEmpty(), () -> "repositories are empty");

        config.getRepositories().forEach(repo -> repositoryMap.put(repo.getName(), repo));
    }

    public VirtualRepository(final VirtualRepositoryConfig config, final Function<String, Repository> repositoryResolver) {
        super(config.getName(), config.getUri());

        for (String repositoryRef : config.getRepositoryRefs()) {
            final Repository repository = repositoryResolver.apply(repositoryRef);

            Objects.requireNonNull(repository, "repository required");

            repositoryMap.put(repository.getName(), repository);
        }

        ConfigValidator.value(repositoryMap, value -> !value.isEmpty(), () -> "VirtualRepository has no Repositories");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        boolean exist = false;

        final String repositoryName = artifactIndexer.findRepository(request);

        if (repositoryName != null) {
            exist = repositoryMap.get(repositoryName).exist(request);
        }
        else {
            for (final Repository repository : repositoryMap.values()) {
                try {
                    exist = repository.exist(request);
                }
                catch (final Exception ex) {
                    getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
                }

                if (exist) {
                    artifactIndexer.storeRepository(request, repository.getName());
                    break;
                }
            }
        }

        return exist;
    }

    @Override
    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        ResourceResponse response = null;

        final String repositoryName = artifactIndexer.findRepository(request);

        if (repositoryName != null) {
            response = repositoryMap.get(repositoryName).getResource(request);
        }
        else {
            for (final Repository repository : repositoryMap.values()) {
                try {
                    response = repository.getResource(request);
                }
                catch (final Exception ex) {
                    getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
                }

                if (response != null) {
                    artifactIndexer.storeRepository(request, repository.getName());
                    break;
                }
            }
        }

        return response;
    }
}
