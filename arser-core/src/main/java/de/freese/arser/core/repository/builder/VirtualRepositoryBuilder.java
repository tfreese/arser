// Created: 14.01.24
package de.freese.arser.core.repository.builder;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.virtual.VirtualRepository;

/**
 * @author Thomas Freese
 */
public class VirtualRepositoryBuilder extends AbstractRepositoryBuilder<VirtualRepositoryBuilder> {
    private List<String> repositoryNames;

    public Repository build(final LifecycleManager lifecycleManager, final Function<String, Repository> repositoryResolver) {
        validateName();

        Objects.requireNonNull(repositoryNames, "repositoryNames required");

        if (repositoryNames.isEmpty()) {
            final String message = String.format("Ignoring VirtualRepository '%s', no repositories configured", getName());
            throw new IllegalStateException(message);
        }

        final VirtualRepository virtualRepository = new VirtualRepository(getName());

        for (final String repositoryName : repositoryNames) {
            final Repository repository = repositoryResolver.apply(repositoryName);

            if (repository == null) {
                getLogger().error("Repository not found or configured: {}", repositoryName);
                continue;
            }

            if (repository instanceof final VirtualRepository vr) {
                getLogger().error("A VirtualRepository can not contain another VirtualRepository: {}", vr.getName());
                continue;
            }

            virtualRepository.add(repository);
        }

        lifecycleManager.add(virtualRepository);

        return virtualRepository;
    }

    public VirtualRepositoryBuilder repositoryNames(final List<String> repositoryNames) {
        this.repositoryNames = repositoryNames != null ? List.copyOf(repositoryNames) : null;

        return this;
    }
}
