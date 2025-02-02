// Created: 22.07.23
package de.freese.arser.jre.server;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

import de.freese.arser.Arser;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.utils.ArserThreadFactory;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpServer extends AbstractLifecycle {
    private final Arser arser;
    private final ServerConfig serverConfig;

    private ExecutorService executorService;
    private HttpClient httpClient;
    private HttpServer httpServer;

    public JreHttpServer(final Arser arser, final ServerConfig serverConfig) {
        super();

        this.arser = Objects.requireNonNull(arser, "arser required");
        this.serverConfig = Objects.requireNonNull(serverConfig, "serverConfig required");
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
        final String threadNamePattern = serverConfig.getThreadNamePattern();
        final int threadPoolCoreSize = serverConfig.getThreadPoolCoreSize();
        final int threadPoolMaxSize = serverConfig.getThreadPoolMaxSize();

        executorService = new ThreadPoolExecutor(threadPoolCoreSize, threadPoolMaxSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ArserThreadFactory(threadNamePattern));

        // httpServer = HttpsServer.create(new InetSocketAddress(port), 0);
        // if (httpServer instanceof HttpsServer https) {
        //     https.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()) {
        //         @Override
        //         public void configure(final HttpsParameters params) {
        //             super.configure(params);
        //         }
        //     });
        // }

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(30));

        httpClient = httpClientBuilder.build();

        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(executorService);
        httpServer.createContext("/", new JreHttpServerHandler(arser, httpClient));

        httpServer.start();
        // new Thread(httpServer::start, "arser").start();
    }

    @Override
    protected void doStop() throws Exception {
        // httpContexts.clear();
        httpServer.stop(3);

        httpClient.close();
        httpClient = null;

        ArserUtils.shutdown(executorService, getLogger());
    }
}
