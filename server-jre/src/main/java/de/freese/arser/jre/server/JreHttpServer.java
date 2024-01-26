// Created: 22.07.23
package de.freese.arser.jre.server;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

import de.freese.arser.config.ServerConfig;
import de.freese.arser.core.Arser;
import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.utils.ArserThreadFactory;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpServer extends AbstractLifecycle {
    private Arser arser;
    private ExecutorService executorService;
    private HttpServer httpServer;
    private ServerConfig serverConfig;

    public JreHttpServer arser(final Arser arser) {
        this.arser = Objects.requireNonNull(arser, "arser required");

        return this;
    }

    public JreHttpServer serverConfig(final ServerConfig serverConfig) {
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

        assertNotNull(arser, () -> "arser");
        assertNotNull(serverConfig, () -> "ServerConfig");

        final int port = assertValue(serverConfig.getPort(), value -> value <= 0, () -> "Port has invalid range");
        final int threadPoolCoreSize = assertValue(serverConfig.getThreadPoolCoreSize(), value -> value <= 0, () -> "ThreadPoolCoreSize has invalid range");
        final int threadPoolMaxSize = assertValue(serverConfig.getThreadPoolMaxSize(), value -> value <= 0, () -> "ThreadPoolMaxSize has invalid range");
        final String threadNamePattern = assertNotNull(serverConfig.getThreadNamePattern(), () -> "ThreadNamePattern");

        this.executorService = new ThreadPoolExecutor(threadPoolCoreSize, threadPoolMaxSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ArserThreadFactory(threadNamePattern));

        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer.setExecutor(executorService);

        this.httpServer.createContext("/", new JreHttpServerHandler(arser));

        this.httpServer.start();
        //        new Thread(this.httpServer::start, "arser").start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        //        this.httpContexts.clear();
        this.httpServer.stop(3);

        ArserUtils.shutdown(this.executorService, getLogger());
    }
}
