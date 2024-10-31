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

import de.freese.arser.blobstore.file.FileBlobStore;
import de.freese.arser.blobstore.jdbc.JdbcBlobStore;
import de.freese.arser.core.Arser;
import de.freese.arser.core.component.BlobStoreComponent;
import de.freese.arser.core.component.DatasourceComponent;
import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.config.ArserConfig;
import de.freese.arser.core.config.DatabaseStoreConfig;
import de.freese.arser.core.config.FileStoreConfig;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.CachedRepository;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;
import de.freese.arser.jre.repository.remote.JreHttpClientRemoteRepository;
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
            // System.setProperty("jdk.httpclient.HttpClient.log", "all");
            System.setProperty("jdk.httpclient.HttpClient.log", "requests");
        }

        try {
            final URI configUri = findConfigFile(args);
            LOGGER.info("load settings from {}", configUri);

            final ArserConfig arserConfig;

            try (InputStream inputStream = configUri.toURL().openStream()) {
                arserConfig = ArserConfig.fromXml(inputStream);
            }

            final Arser arser = createArser(arserConfig);

            // ArserUtils.setupProxy();

            // Logger root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            // root.setLevel(Level.INFO);

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

    private static Arser createArser(final ArserConfig arserConfig) {
        final LifecycleManager lifecycleManager = new LifecycleManager();

        final JreHttpClientComponent httpClientComponent = new JreHttpClientComponent(arserConfig.getHttpClientConfig());
        lifecycleManager.add(httpClientComponent);

        final Arser arser = new Arser(lifecycleManager);

        // LocalRepository
        arserConfig.getRepositoryConfigsLocal().forEach(config -> {
            final Repository repository = new FileRepository(config);
            lifecycleManager.add(repository);

            arser.addRepository(repository);
        });

        // RemoteRepository
        arserConfig.getRepositoryConfigsRemote().forEach(config -> {
            Repository repository = new JreHttpClientRemoteRepository(config, httpClientComponent::getHttpClient, arserConfig.getTempDir());

            if (config.getStoreConfig() instanceof FileStoreConfig fsc) {
                final BlobStoreComponent blobStoreComponent = new BlobStoreComponent(new FileBlobStore(fsc.getUri()));
                lifecycleManager.add(blobStoreComponent);

                repository = new CachedRepository(repository, blobStoreComponent.getBlobStore());
                lifecycleManager.add(repository);
            }
            else if (config.getStoreConfig() instanceof DatabaseStoreConfig dbsc) {
                final DatasourceComponent datasourceComponent = new DatasourceComponent(dbsc);
                lifecycleManager.add(datasourceComponent);

                final BlobStoreComponent blobStoreComponent = new BlobStoreComponent(new JdbcBlobStore(datasourceComponent::getDataSource));
                lifecycleManager.add(blobStoreComponent);

                repository = new CachedRepository(repository, blobStoreComponent.getBlobStore());
                lifecycleManager.add(repository);
            }
            else {
                lifecycleManager.add(repository);
            }

            arser.addRepository(repository);
        });

        // VirtualRepository
        arserConfig.getRepositoryConfigsVirtual().forEach(config -> {
            final Repository repository = new VirtualRepository(config, arser::getRepository);
            lifecycleManager.add(repository);

            arser.addRepository(repository);
        });

        // Server at last
        final JreHttpServer proxyServer = new JreHttpServer(arser, arserConfig.getServerConfig());
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
