// Created: 22.07.23
package de.freese.arser.core.server;

import java.util.Objects;

import de.freese.arser.config.ServerConfig;
import de.freese.arser.core.Arser;
import de.freese.arser.core.lifecycle.AbstractLifecycle;

/**
 * @author Thomas Freese
 */
public abstract class AbstractArserServer extends AbstractLifecycle implements ArserServer {
    private Arser arser;
    private ServerConfig serverConfig;

    @Override
    public ArserServer setArser(final Arser arser) {
        this.arser = Objects.requireNonNull(arser, "arser required");

        return this;
    }

    @Override
    public ArserServer setConfig(final ServerConfig serverConfig) {
        this.serverConfig = Objects.requireNonNull(serverConfig, "serverConfig required");

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

        checkNotNull(arser, "arser");
        checkNotNull(serverConfig, "ServerConfig");
        checkValue(serverConfig.getPort(), value -> value <= 0 ? ("Port has invalid range: " + value) : null);
        checkValue(serverConfig.getThreadPoolCoreSize(), value -> value <= 0 ? ("ThreadPoolCoreSize has invalid range: " + value) : null);
        checkValue(serverConfig.getThreadPoolMaxSize(), value -> value <= 0 ? ("ThreadPoolMaxSize has invalid range: " + value) : null);
        checkNotNull(serverConfig.getThreadNamePattern(), "ThreadNamePattern");
    }

    protected Arser getArser() {
        return arser;
    }

    protected ServerConfig getServerConfig() {
        return serverConfig;
    }
}
