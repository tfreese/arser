package de.freese.arser.repository.virtual;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.repository.AbstractRepositoryBuilder;
import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 * @since 04.07.26
 */
public final class VirtualRepositoryBuilder extends AbstractRepositoryBuilder<VirtualRepositoryBuilder, Repository> {
    private final Set<String> repositoryRefs = new LinkedHashSet<>();

    private Function<String, Repository> repositoryProvider;

    VirtualRepositoryBuilder() {
        super();
    }

    public VirtualRepositoryBuilder addRepositoryRef(final String repositoryName) {
        if (repositoryRefs.contains(repositoryName)) {
            throw new IllegalStateException("RepositoryName already exists: " + repositoryName);
        }

        this.repositoryRefs.add(Objects.requireNonNull(repositoryName, "repositoryName required"));

        return self();
    }

    @Override
    public Repository build(final LifeCycleRegistry lifeCycleRegistry) throws Exception {
        Objects.requireNonNull(getUri(), "uri required");
        Objects.requireNonNull(getName(), "name required");
        Objects.requireNonNull(repositoryProvider, "repositoryProvider required");

        if (repositoryRefs.isEmpty()) {
            throw new IllegalStateException("No repositories names are defined");
        }

        final Map<String, Repository> repositoryMap = new LinkedHashMap<>();

        for (final String repositoryName : repositoryRefs) {
            final Repository repository = repositoryProvider.apply(repositoryName);

            if (repository == null) {
                throw new IllegalStateException("Repository not found: " + repositoryName);
            }

            repositoryMap.put(repositoryName, repository);
        }

        return new VirtualRepository(getUri(), getName(), repositoryMap);
    }

    public VirtualRepositoryBuilder repositoryProvider(final Function<String, Repository> repositoryProvider) {
        this.repositoryProvider = Objects.requireNonNull(repositoryProvider, "repositoryProvider required");

        return self();
    }

    @Override
    protected VirtualRepositoryBuilder self() {
        return this;
    }
}
