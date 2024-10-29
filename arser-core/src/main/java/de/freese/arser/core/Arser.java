// Created: 20.01.24
package de.freese.arser.core;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;

/**
 * ARtifact-SERvice<br>
 * Inspired by <a href="https://github.com/sonatype/nexus-public">nexus-public</a>
 *
 * @author Thomas Freese
 */
public final class Arser extends AbstractLifecycle {
    private static final Pattern PATTERN_REPO_NAME = Pattern.compile("([a-z0-9\\-_])+");

    public static void validateRepositoryName(final String name) {
        if (!PATTERN_REPO_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("name must match the pattern: " + PATTERN_REPO_NAME);
        }
    }

    private final LifecycleManager lifecycleManager;
    private final Map<String, Repository> repositoryMap = new HashMap<>();

    public Arser() {
        this(null);
    }

    public Arser(final LifecycleManager lifecycleManager) {
        super();

        this.lifecycleManager = lifecycleManager;
    }

    public void addRepository(final Repository repository) {
        Objects.requireNonNull(repository, "repository required");

        final String name = repository.getName();

        validateRepositoryName(name);

        if (repositoryMap.containsKey(name)) {
            throw new IllegalStateException("repository already exist: " + name);
        }

        repositoryMap.put(name, repository);
    }

    public boolean exist(final ResourceRequest request) throws Exception {
        final Repository repository = getRepository(request.getContextRoot());

        return repository.exist(request);
    }

    public Repository getRepository(final String name) {
        final Repository repository = repositoryMap.get(name);

        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + name);
        }

        return repository;
    }

    public int getRepositoryCount() {
        return repositoryMap.size();
    }

    public ResourceResponse getResource(final ResourceRequest request) throws Exception {
        final Repository repository = getRepository(request.getContextRoot());

        return repository.getResource(request);
    }

    public void write(final ResourceRequest request, final InputStream inputStream) throws Exception {
        final Repository repository = getRepository(request.getContextRoot());

        if (!repository.isWriteable()) {
            throw new IllegalStateException("Repository is not writeable: " + repository.getName());
        }

        repository.write(request, inputStream);
    }

    @Override
    protected void doStart() throws Exception {
        if (lifecycleManager != null) {
            lifecycleManager.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (lifecycleManager != null) {
            lifecycleManager.stop();
        }
    }
}
