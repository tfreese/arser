// Created: 22.07.23
package de.freese.arser.core.server;

import java.util.LinkedHashMap;
import java.util.Map;

import de.freese.arser.config.ServerConfig;
import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.repository.Repository;

/**
 * @author Thomas Freese
 */
public abstract class AbstractArserServer extends AbstractLifecycle implements ArserServer {

    private final Map<String, Repository> contextRoots = new LinkedHashMap<>();

    private ServerConfig serverConfig;

    @Override
    public ArserServer addContextRoot(final String contextRoot, final Repository repository) {
        checkNotNull(contextRoot, "ContextRoot");
        checkNotNull(repository, "Repository");

        if (contextRoots.containsKey(repository.getName())) {
            throw new IllegalArgumentException("ContextRoot already exist: " + repository.getName());
        }

        contextRoots.put(contextRoot, repository);

        return this;
    }

    @Override
    public ArserServer setConfig(final ServerConfig serverConfig) {
        this.serverConfig = serverConfig;

        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [");
        sb.append("port=").append(serverConfig != null ? serverConfig.getPort() : "-1");
        sb.append(']');

        return sb.toString();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        checkNotNull(serverConfig, "ServerConfig");
        checkValue(serverConfig.getPort(), value -> value <= 0 ? ("Port has invalid range: " + value) : null);
        checkValue(serverConfig.getThreadPoolCoreSize(), value -> value <= 0 ? ("ThreadPoolCoreSize has invalid range: " + value) : null);
        checkValue(serverConfig.getThreadPoolMaxSize(), value -> value <= 0 ? ("ThreadPoolMaxSize has invalid range: " + value) : null);
        checkNotNull(serverConfig.getThreadNamePattern(), "ThreadNamePattern");
        checkValue(contextRoots, value -> value.isEmpty() ? "ContextRoots are not defined" : null);
    }

    protected Map<String, Repository> getContextRoots() {
        return Map.copyOf(contextRoots);
    }

    protected ServerConfig getServerConfig() {
        return serverConfig;
    }
}
