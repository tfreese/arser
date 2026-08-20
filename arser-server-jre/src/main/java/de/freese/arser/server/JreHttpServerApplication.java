package de.freese.arser.server;

import java.net.URL;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.freese.arser.api.Arser;
import de.freese.arser.api.ArserConfig;
import de.freese.arser.component.DefaultLifeCycleRegistry;
import de.freese.arser.component.LifeCycleRegistry;

/**
 * @author Thomas Freese
 * @since 27.07.23
 */
public final class JreHttpServerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(JreHttpServerApplication.class);

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

            final URL xmlFile = Thread.currentThread().getContextClassLoader().getResource("arser-config.xml");
            final URL xsdFile = Thread.currentThread().getContextClassLoader().getResource("config/arser-config.xsd");

            Objects.requireNonNull(xmlFile, "xmlFile required");
            Objects.requireNonNull(xsdFile, "xsdFile required");

            final ArserConfig arserConfig = ArserConfig.fromXml(xmlFile, xsdFile);

            // final ArserConfig arserConfig = ArserConfig.builder()
            //         .serverConfig(ServerConfig.builder()
            //                 .port(8080)
            //                 .threadPoolConfig(ThreadPoolConfig.builderServerDefault()
            //                         .build())
            //                 .build())
            //         .addHttpRepositoryConfig(HttpRepositoryConfig.builder()
            //                 .uri(URI.create("https://repo1.maven.org/maven2"))
            //                 .name("maven-central")
            //                 .withRetrying(3, Duration.ofSeconds(2L))
            //                 .cachingPath(WORKING_PATH.resolve("cache").resolve("maven-central"))
            //                 .withLogging()
            //                 .build())
            //         .addHttpRepositoryConfig(HttpRepositoryConfig.builder()
            //                 .uri(URI.create("https://repo.gradle.org/gradle/libs-releases"))
            //                 .name("gradle-libs-releases")
            //                 .withRetrying(3, Duration.ofSeconds(2L))
            //                 .cachingPath(WORKING_PATH.resolve("cache").resolve("gradle-libs-releases"))
            //                 .withLogging()
            //                 .build())
            //         .addFileRepositoryConfig(FileRepositoryConfig.builder()
            //                 .uri(WORKING_PATH.resolve("local").resolve("snapshots").toUri())
            //                 .name("snapshots")
            //                 .readOnly(false)
            //                 .withLogging()
            //                 .build())
            //         .addVirtualRepositoryConfig(VirtualRepositoryConfig.builder()
            //                 .uri(URI.create("virtual://public"))
            //                 .name("public")
            //                 .addRepositoryRef("maven-central")
            //                 .addRepositoryRef("gradle-libs-releases")
            //                 .addRepositoryRef("snapshots")
            //                 .build())
            //         .build();

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
