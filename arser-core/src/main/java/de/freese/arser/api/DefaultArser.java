package de.freese.arser.api;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 * @since 04.07.26
 */
public final class DefaultArser implements Arser {
    private final ArserConfig config;
    private final Map<String, Repository> repositories = new ConcurrentHashMap<>();

    DefaultArser(final ArserConfig config, final Map<String, Repository> repositories) {
        super();

        this.config = Objects.requireNonNull(config, "config required");
        this.repositories.putAll(repositories);
    }

    @Override
    public ArserResult download(final String repositoryName, final ArserRequest arserRequest) {
        return getRepository(repositoryName).download(arserRequest);
    }

    @Override
    public ArserResult exist(final String repositoryName, final ArserRequest arserRequest) {
        return getRepository(repositoryName).exist(arserRequest);
    }

    @Override
    public ArserConfig getConfig() {
        return config;
    }

    @Override
    public Map<String, Repository> getRepositories() {
        return new TreeMap<>(repositories);
    }

    @Override
    public ArserResult upload(final String repositoryName, final ArserRequest arserRequest, final InputStream inputStream) {
        return getRepository(repositoryName).upload(arserRequest, inputStream);
    }

    Repository getRepository(final String repositoryName) {
        final Repository repository = repositories.get(repositoryName);

        if (repository == null) {
            throw new IllegalStateException("Repository not found: " + repositoryName);
        }

        return repository;
    }
}
