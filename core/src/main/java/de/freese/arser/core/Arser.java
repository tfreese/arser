// Created: 20.01.24
package de.freese.arser.core;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.builder.LocalRepositoryBuilder;
import de.freese.arser.core.repository.builder.RemoteRepositoryBuilder;
import de.freese.arser.core.repository.builder.VirtualRepositoryBuilder;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.server.ArserServer;
import de.freese.arser.core.server.jre.JreHttpServer;
import de.freese.arser.core.settings.ArserSettings;

/**
 * ARtifact-SERvice<br>
 * Inspired by <a href="https://github.com/sonatype/nexus-public">nexus-public</a>
 *
 * @author Thomas Freese
 */
public final class Arser extends AbstractLifecycle {
    private static final Pattern PATTERN_REPO_NAME = Pattern.compile("([a-z0-9\\-_])+");

    /**
     * Only for Test-Cases.
     */
    public static Arser newInstance(final LifecycleManager lifecycleManager) {
        return new Arser(lifecycleManager);
    }

    public static Arser newInstance(final ArserSettings settings) {
        final LifecycleManager lifecycleManager = new LifecycleManager();

        final JreHttpClientComponent httpClientComponent = new JreHttpClientComponent(settings.getHttpClientConfig());
        lifecycleManager.add(httpClientComponent);

        final Arser arser = new Arser(lifecycleManager);

        // LocalRepository
        settings.getLocalRepositories().forEach(localRepoConfig -> {
            final LocalRepositoryBuilder builder = new LocalRepositoryBuilder();
            builder.setName(localRepoConfig.getName());
            builder.setUri(URI.create(localRepoConfig.getPath()));
            builder.setWriteable(localRepoConfig.isWriteable());

            arser.addRepository(builder.build(lifecycleManager));
        });

        // RemoteRepository
        settings.getRemoteRepositories().forEach(remoteRepoConfig -> {
            final RemoteRepositoryBuilder builder = new RemoteRepositoryBuilder();
            builder.setName(remoteRepoConfig.getName());
            builder.setUri(URI.create(remoteRepoConfig.getUri()));
            builder.setStoreConfig(remoteRepoConfig.getStoreConfig());

            arser.addRepository(builder.build(lifecycleManager, httpClientComponent));
        });

        // VirtualRepository
        settings.getVirtualRepositories().forEach(virtualRepoConfig -> {
            final VirtualRepositoryBuilder builder = new VirtualRepositoryBuilder();
            builder.setName(virtualRepoConfig.getName());
            builder.setRepositoryNames(virtualRepoConfig.getRepositoryNames());

            arser.addRepository(builder.build(lifecycleManager, arser::getRepository));
        });

        // Server at last
        final ArserServer proxyServer = new JreHttpServer().setConfig(settings.getServerConfig()).setArser(arser);
        lifecycleManager.add(proxyServer);

        return arser;
    }

    public static void validateRepositoryName(final String name) {
        if (!PATTERN_REPO_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("name must match the pattern: " + PATTERN_REPO_NAME);
        }
    }

    private final LifecycleManager lifecycleManager;
    private final Map<String, Repository> repositoryMap = new HashMap<>();

    private Arser(final LifecycleManager lifecycleManager) {
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

    public ResourceResponse getResource(final ResourceRequest request) throws Exception {
        final Repository repository = getRepository(request.getContextRoot());

        return repository.getInputStream(request);
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
        lifecycleManager.start();
    }

    @Override
    protected void doStop() throws Exception {
        lifecycleManager.stop();
    }

    private Repository getRepository(final String name) {
        final Repository repository = repositoryMap.get(name);

        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + name);
        }

        return repository;
    }
}
