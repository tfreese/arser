// Created: 14.01.24
package de.freese.arser.core.repository.builder;

import java.util.List;
import java.util.Objects;

import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.RepositoryManager;
import de.freese.arser.core.repository.configurer.VirtualRepositoryConfigurer;
import de.freese.arser.core.repository.virtual.DefaultVirtualRepository;

/**
 * @author Thomas Freese
 */
public class VirtualRepositoryBuilder extends AbstractRepositoryBuilder implements VirtualRepositoryConfigurer {
    private List<String> repositoryNames;

    public Repository build(final LifecycleManager lifecycleManager, final RepositoryManager repositoryManager) {
        validateName();

        Objects.requireNonNull(repositoryNames, "repositoryNames required");

        if (repositoryNames.isEmpty()) {
            final String message = String.format("Ignoring VirtualRepository '%s', no repositories configured", getName());
            throw new IllegalStateException(message);
        }

        final DefaultVirtualRepository virtualRepository = new DefaultVirtualRepository(getName());

        for (final String repositoryName : repositoryNames) {
            final Repository repository = repositoryManager.getRepository(repositoryName);

            if (repository == null) {
                getLogger().error("Repository not found or configured: {}", repositoryName);
                continue;
            }

            if (repository instanceof final DefaultVirtualRepository vr) {
                getLogger().error("A VirtualRepository can not contain another VirtualRepository: {}", vr.getName());
                continue;
            }

            virtualRepository.add(repository);
        }

        lifecycleManager.add(virtualRepository);

        return virtualRepository;
    }

    @Override
    public void setRepositoryNames(final List<String> repositoryNames) {
        this.repositoryNames = repositoryNames != null ? List.copyOf(repositoryNames) : null;
    }
}
