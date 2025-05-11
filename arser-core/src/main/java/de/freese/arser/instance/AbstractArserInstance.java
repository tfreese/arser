// Created: 11 Mai 2025
package de.freese.arser.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.config.ArserConfig;
import de.freese.arser.config.ConfigValidator;
import de.freese.arser.core.repository.Repository;

/**
 * @author Thomas Freese
 */
public abstract class AbstractArserInstance implements ArserInstance {
    private final ArserConfig config;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final Map<String, Repository> repositoryMap = new HashMap<>();

    protected AbstractArserInstance(final String name, final ArserConfig config) {
        super();

        this.name = Objects.requireNonNull(name, "name required");
        this.config = Objects.requireNonNull(config, "config required");
    }

    @Override
    public void addRepository(final Repository repository) {
        Objects.requireNonNull(repository, "repository required");

        final String contextRoot = repository.getContextRoot();

        ConfigValidator.contextRoot(contextRoot);

        if (repositoryMap.containsKey(contextRoot)) {
            throw new IllegalStateException("repository already exist for contextRoot: " + contextRoot);
        }

        repositoryMap.put(contextRoot, repository);
    }

    @Override
    public void forEach(final Consumer<Repository> consumer) {
        repositoryMap.values().forEach(consumer);
    }

    @Override
    public ArserConfig getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Repository getRepository(final String contextRoot) {
        return repositoryMap.get(contextRoot);
    }

    @Override
    public int getRepositoryCount() {
        return repositoryMap.size();
    }

    protected Logger getLogger() {
        return logger;
    }
}
