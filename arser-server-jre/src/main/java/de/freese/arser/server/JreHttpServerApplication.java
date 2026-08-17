package de.freese.arser.server;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.freese.arser.api.Arser;
import de.freese.arser.api.ArserConfig;
import de.freese.arser.component.DefaultLifeCycleRegistry;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.config.ThreadPoolConfig;
import de.freese.arser.repository.file.FileRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.virtual.VirtualRepositoryConfig;

/**
 * @author Thomas Freese
 * @since 27.07.23
 */
public final class JreHttpServerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(JreHttpServerApplication.class);

    private static final Path WORKING_PATH = Path.of(System.getProperty("java.io.tmpdir"), "arser");

    static void main() {
        try {
            // Redirect Java-Util-Logger to Slf4J.
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            if (LoggerFactory.getLogger("jdk.httpclient.HttpClient").isDebugEnabled()) {
                // System.setProperty("jdk.httpclient.HttpClient.log", "all");
                System.setProperty("jdk.httpclient.HttpClient.log", "requests");
            }

            final LifeCycleRegistry lifeCycleRegistry = new DefaultLifeCycleRegistry();

            final ArserConfig arserConfig = ArserConfig.builder()
                    .addHttpRepositoryConfig(HttpRepositoryConfig.builder()
                            .uri(URI.create("https://repo1.maven.org/maven2"))
                            .name("maven-central")
                            .withRetrying(3, Duration.ofSeconds(2L))
                            .cachingPath(WORKING_PATH.resolve("maven-central-cache"))
                            .withLogging()
                            .build())
                    .addFileRepositoryConfig(FileRepositoryConfig.builder()
                            .uri(WORKING_PATH.resolve("snapshots").toUri())
                            .name("deploy-snapshots")
                            .readOnly(false)
                            .withLogging()
                            .build())
                    .addVirtualRepositoryConfig(VirtualRepositoryConfig.builder()
                            .uri(URI.create("virtual://public"))
                            .name("public")
                            .addRepositoryRef("maven-central")
                            .addRepositoryRef("deploy-snapshots")
                            .build())
                    .serverConfig(ServerConfig.builder()
                            .port(8080)
                            .threadPoolConfig(ThreadPoolConfig.builderServerDefault()
                                    .build())
                            .build())
                    .build();

            final Arser arser = Arser.from(arserConfig, lifeCycleRegistry);

            final JreHttpServer jreHttpServer = new JreHttpServer(arser);
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
        catch (final Exception ex) {
            LOGGER.error(ex.getMessage(), ex);

            System.exit(-1);
        }
    }

    private JreHttpServerApplication() {
        super();
    }
}
