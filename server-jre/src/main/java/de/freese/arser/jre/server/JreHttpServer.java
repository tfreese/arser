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
import de.freese.arser.config.ThreadPoolConfig;
import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.utils.ArserThreadFactory;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.instance.ArserInstance;

/**
 * @author Thomas Freese
 */
public class JreHttpServer extends AbstractLifecycle {
    private final ArserInstance arserInstance;
    private final ServerConfig serverConfig;

    private ExecutorService executorService;
    private HttpServer httpServer;

    public JreHttpServer(final ArserInstance arserInstance) {
        super();

        this.arserInstance = Objects.requireNonNull(arserInstance, "arserInstance required");

        serverConfig = arserInstance.getConfig().getServerConfig();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [");
        sb.append("port=").append(serverConfig.getPort());
        sb.append(']');

        return sb.toString();
    }

    @Override
    protected void doStart() throws Exception {
        final int port = serverConfig.getPort();
        final ThreadPoolConfig threadPoolConfig = serverConfig.getThreadPoolConfig();

        executorService = new ThreadPoolExecutor(threadPoolConfig.getCoreSize(),
                threadPoolConfig.getMaxSize(),
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ArserThreadFactory(threadPoolConfig.getNamePattern()));

        // httpServer = HttpsServer.create(new InetSocketAddress(port), 0);
        // if (httpServer instanceof HttpsServer https) {
        //     https.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()) {
        //         @Override
        //         public void configure(final HttpsParameters params) {
        //             super.configure(params);
        //         }
        //     });
        // }

        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(executorService);
        httpServer.createContext("/", new JreHttpServerHandler(arserInstance));

        httpServer.start();
        // new Thread(httpServer::start, "arser").start();
    }

    @Override
    protected void doStop() throws Exception {
        // httpContexts.clear();
        httpServer.stop(3);

        ArserUtils.shutdown(executorService, getLogger());
    }
}
