package de.freese.arser.server;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.freese.arser.api.Arser;
import de.freese.arser.component.AbstractComponent;
import de.freese.arser.component.DefaultLifeCycleRegistry;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.config.ThreadPoolConfig;
import de.freese.arser.repository.http.HttpRepository;
import de.freese.arser.repository.http.HttpRepositoryBuilder;
import de.freese.arser.utils.ArserThreadFactory;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 * @since 22.07.23
 */
public class JreHttpServer extends AbstractComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(JreHttpServer.class);

    static void main() throws Exception {
        // Redirect Java-Util-Logger to Slf4J.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        if (LoggerFactory.getLogger("jdk.httpclient.HttpClient").isDebugEnabled()) {
            // System.setProperty("jdk.httpclient.HttpClient.log", "all");
            System.setProperty("jdk.httpclient.HttpClient.log", "requests");
        }

        final LifeCycleRegistry lifeCycleRegistry = new DefaultLifeCycleRegistry();

        final HttpRepositoryBuilder httpRepositoryBuilder = HttpRepository.builder()
                .uri(URI.create("https://repo1.maven.org/maven2"))
                .name("central")
                .withRetrying(3, Duration.ofSeconds(2L))
                .withCaching(Duration.ofMinutes(5L))
                .withLogging();

        final Arser arser = Arser.builder()
                .add(httpRepositoryBuilder)
                .build(lifeCycleRegistry);

        final JreHttpServer jreHttpServer = new JreHttpServer(arser, ServerConfig.builder().port(8080).build());
        lifeCycleRegistry.register(jreHttpServer);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                lifeCycleRegistry.stop();
            }
            catch (final Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }, "Shutdown"));

        lifeCycleRegistry.start();
    }

    private final Arser arser;
    private final ServerConfig serverConfig;
    private ExecutorService executorService;
    private HttpServer httpServer;

    public JreHttpServer(final Arser arser, final ServerConfig serverConfig) {
        super();

        this.arser = Objects.requireNonNull(arser, "arser required");
        this.serverConfig = Objects.requireNonNull(serverConfig, "serverConfig required");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + " ["
                + "port=" + serverConfig.getPort()
                + ']';
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
        httpServer.createContext("/", new JreHttpServerHandler(arser));

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
