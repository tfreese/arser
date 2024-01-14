// Created: 22.07.23
package de.freese.arser.core.repository;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import de.freese.arser.core.component.AbstractComponent;
import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.builder.LocalRepositoryBuilder;
import de.freese.arser.core.repository.builder.RemoteRepositoryBuilder;
import de.freese.arser.core.repository.builder.VirtualRepositoryBuilder;
import de.freese.arser.core.repository.configurer.LocalRepositoryConfigurer;
import de.freese.arser.core.repository.configurer.RemoteRepositoryConfigurer;
import de.freese.arser.core.repository.configurer.VirtualRepositoryConfigurer;

/**
 * @author Thomas Freese
 */
public class RepositoryManager extends AbstractComponent {

    private final LifecycleManager lifecycleManager;
    private final Map<String, Repository> repositories = new TreeMap<>();

    public RepositoryManager(final LifecycleManager lifecycleManager) {
        super();

        this.lifecycleManager = lifecycleManager;
    }

    public void add(final Repository repository) {
        checkNotNull(repository, "Repository");

        if (repositories.containsKey(repository.getName())) {
            throw new IllegalArgumentException("Repository already exist: " + repository.getName());
        }

        repositories.put(repository.getName(), repository);
    }

    public void addLocal(final Consumer<LocalRepositoryConfigurer> configurer) {
        final LocalRepositoryBuilder builder = new LocalRepositoryBuilder();

        configurer.accept(builder);

        add(builder.build(lifecycleManager));
    }

    public void addRemote(final Consumer<RemoteRepositoryConfigurer> configurer, final JreHttpClientComponent httpClientComponent) {
        final RemoteRepositoryBuilder builder = new RemoteRepositoryBuilder();

        configurer.accept(builder);

        add(builder.build(lifecycleManager, httpClientComponent));
    }

    public void addVirtual(final Consumer<VirtualRepositoryConfigurer> configurer) {
        final VirtualRepositoryBuilder builder = new VirtualRepositoryBuilder();

        configurer.accept(builder);

        add(builder.build(lifecycleManager, this));
    }

    public Stream<Repository> getRepositories() {
        return repositories.values().stream().filter(Objects::nonNull);
    }

    public Repository getRepository(final String name) {
        return repositories.get(name);
    }
}
