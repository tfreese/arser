// Created: 22.07.23
package de.freese.arser.core.repository;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import de.freese.arser.core.config.ConfigValidator;
import de.freese.arser.core.config.VirtualRepositoryConfig;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;

/**
 * @author Thomas Freese
 */
public class VirtualRepository extends AbstractRepository {

    private final CopyOnWriteArrayList<Repository> repositories = new CopyOnWriteArrayList<>();

    public VirtualRepository(final VirtualRepositoryConfig config) {
        super(config.getName(), config.getUri());

        ConfigValidator.value(config.getRepositories(), value -> value != null && !value.isEmpty(), () -> "repositories are empty");

        repositories.addAll(config.getRepositories());
    }

    public VirtualRepository(final VirtualRepositoryConfig config, final Function<String, Repository> repositoryResolver) {
        super(config.getName(), config.getUri());

        for (String repositoryRef : config.getRepositoryRefs()) {
            final Repository repository = repositoryResolver.apply(repositoryRef);

            repositories.add(Objects.requireNonNull(repository, "repository required"));
        }

        ConfigValidator.value(repositories, value -> value != null && !value.isEmpty(), () -> "repositories are empty");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) {
        boolean exist = false;

        for (final Repository repository : repositories) {
            try {
                exist = repository.exist(request);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (exist) {
                break;
            }
        }

        return exist;
    }

    @Override
    protected ResourceResponse doGetResource(final ResourceRequest request) {
        ResourceResponse response = null;

        for (final Repository repository : repositories) {
            try {
                response = repository.getResource(request);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (response != null) {
                break;
            }
        }

        return response;
    }
}
