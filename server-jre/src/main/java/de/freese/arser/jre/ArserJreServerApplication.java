// Created: 27.07.23
package de.freese.arser.jre;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.freese.arser.core.Arser;
import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.builder.LocalRepositoryBuilder;
import de.freese.arser.core.repository.builder.VirtualRepositoryBuilder;
import de.freese.arser.core.settings.ArserSettings;
import de.freese.arser.jre.repository.builder.RemoteRepositoryBuilder;
import de.freese.arser.jre.server.JreHttpServer;

/**
 * @author Thomas Freese
 */
public final class ArserJreServerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArserJreServerApplication.class);

    public static void main(final String[] args) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Working Directory: {}", System.getProperty("user.dir"));
            LOGGER.info("Process User: {}", System.getProperty("user.name"));
        }

        // Redirect Java-Util-Logger to Slf4J.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        if (LoggerFactory.getLogger("jdk.httpclient.HttpClient").isDebugEnabled()) {
            //            System.setProperty("jdk.httpclient.HttpClient.log", "all");
            System.setProperty("jdk.httpclient.HttpClient.log", "requests");
        }

        try {
            final URI configUri = findConfigFile(args);
            LOGGER.info("load settings from {}", configUri);

            final ArserSettings arserSettings;

            try (InputStream inputStream = configUri.toURL().openStream()) {
                arserSettings = ArserSettings.fromXml(inputStream);
            }

            final Arser arser = createArser(arserSettings);

            // ArserUtils.setupProxy();

            //        Logger root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            //        root.setLevel(Level.INFO);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    arser.stop();
                }
                catch (final Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }, "Shutdown"));

            arser.start();
        }
        catch (final Exception ex) {
            LOGGER.error(ex.getMessage(), ex);

            System.exit(-1);
        }
    }

    private static Arser createArser(final ArserSettings arserSettings) {
        final LifecycleManager lifecycleManager = new LifecycleManager();

        final JreHttpClientComponent httpClientComponent = new JreHttpClientComponent(arserSettings.getHttpClientConfig());
        lifecycleManager.add(httpClientComponent);

        final Arser arser = new Arser(lifecycleManager);

        // LocalRepository
        arserSettings.getLocalRepositories().forEach(localRepoConfig -> {
            // @formatter:off
            final LocalRepositoryBuilder builder = new LocalRepositoryBuilder()
                    .name(localRepoConfig.getName())
                    .uri(URI.create(localRepoConfig.getPath()))
                    .writeable(localRepoConfig.isWriteable())
                    ;
            // @formatter:on

            arser.addRepository(builder.build(lifecycleManager));
        });

        // RemoteRepository
        arserSettings.getRemoteRepositories().forEach(remoteRepoConfig -> {
            // @formatter:off
            final RemoteRepositoryBuilder builder = new RemoteRepositoryBuilder()
                    .name(remoteRepoConfig.getName())
                    .uri(URI.create(remoteRepoConfig.getUri()))
                    .storeConfig(remoteRepoConfig.getStoreConfig())
                    ;
            // @formatter:off

            arser.addRepository(builder.build(lifecycleManager, httpClientComponent));
        });

        // VirtualRepository
        arserSettings.getVirtualRepositories().forEach(virtualRepoConfig -> {
            // @formatter:off
            final VirtualRepositoryBuilder builder = new VirtualRepositoryBuilder()
                    .name(virtualRepoConfig.getName())
                    .repositoryNames(virtualRepoConfig.getRepositoryNames())
                    ;
            // @formatter:off

            arser.addRepository(builder.build(lifecycleManager, arser::getRepository));
        });

        // Server at last
        final JreHttpServer proxyServer = new JreHttpServer().serverConfig(arserSettings.getServerConfig()).arser(arser);
        lifecycleManager.add(proxyServer);

        return arser;
    }

    private static URI findConfigFile(final String[] args) throws Exception {
        LOGGER.info("Try to find arser-config.xml");

        URI configUri = null;

        if (args != null && args.length == 2) {
            final String parameter = args[0];

            if ("-arser.config".equals(parameter)) {
                configUri = Paths.get(args[1]).toUri();
            }
        }

        if (configUri == null) {
            final String propertyValue = System.getProperty("arser.config");

            if (propertyValue != null) {
                configUri = Paths.get(propertyValue).toUri();
            }
        }

        if (configUri == null) {
            final String envValue = System.getenv("arser.config");

            if (envValue != null) {
                configUri = Paths.get(envValue).toUri();
            }
        }

        if (configUri == null) {
            final URL url = ClassLoader.getSystemResource("arser-config.xml");

            if (url != null) {
                configUri = url.toURI();
            }
        }

        if (configUri == null) {
            final Path path = Path.of("arser-config.xml");

            if (Files.exists(path)) {
                configUri = path.toUri();
            }
        }

        if (configUri == null) {
            LOGGER.error("no arser config file found");
            LOGGER.error("define it as programm argument: -arser.config <ABSOLUTE_PATH>/arser-config.xml");
            LOGGER.error("or as system property: -Darser.config=<ABSOLUTE_PATH>/arser-config.xml");
            LOGGER.error("or as environment variable: set/export arser.config=<ABSOLUTE_PATH>/arser-config.xml");
            LOGGER.error("or in Classpath");
            LOGGER.error("or in directory of the Proxy-Jar.");

            throw new IllegalStateException("no arser config file found");
        }

        return configUri;
    }

    private ArserJreServerApplication() {
        super();
    }
}
