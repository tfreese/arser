// Created: 27.07.23
package de.freese.arser.jre;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.freese.arser.Arser;
import de.freese.arser.config.ArserConfig;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.RemoteRepositoryJreHttpClient;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;
import de.freese.arser.jre.server.JreHttpServer;

/**
 * @author Thomas Freese
 */
public final class ArserJreServerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArserJreServerApplication.class);

    public static void main(final String[] args) {
        // if (LOGGER.isInfoEnabled()) {
        //     LOGGER.info("Working Directory: {}", System.getProperty("user.dir"));
        //     LOGGER.info("Process User: {}", System.getProperty("user.name"));
        // }

        // Redirect Java-Util-Logger to Slf4J.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        if (LoggerFactory.getLogger("jdk.httpclient.HttpClient").isDebugEnabled()) {
            // System.setProperty("jdk.httpclient.HttpClient.log", "all");
            System.setProperty("jdk.httpclient.HttpClient.log", "requests");
        }

        try {
            final ArserConfig arserConfig = ArserConfig.fromXml(args);

            final LifecycleManager lifecycleManager = createArser(arserConfig);

            // ArserUtils.setupProxy();

            // Logger root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            // root.setLevel(Level.INFO);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lifecycleManager.stop();
                }
                catch (final Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }, "Shutdown"));

            lifecycleManager.start();
        }
        catch (final Exception ex) {
            LOGGER.error(ex.getMessage(), ex);

            System.exit(-1);
        }
    }

    private static LifecycleManager createArser(final ArserConfig arserConfig) {
        final LifecycleManager lifecycleManager = new LifecycleManager();

        final Arser arser = new Arser();
        // final List<Repository> publicRepositories = new ArrayList<>();
        //
        // Repository repository = new RemoteRepositoryJreHttpClient("maven-central", URI.create("https://repo1.maven.org/maven2"));
        // lifecycleManager.add(repository);
        // arser.addRepository(repository);
        // publicRepositories.add(repository);
        //
        // repository = new RemoteRepositoryJreHttpClient("gradle-releases", URI.create("https://repo.gradle.org/gradle/libs-releases"));
        // lifecycleManager.add(repository);
        // arser.addRepository(repository);
        // publicRepositories.add(repository);
        //
        // repository = new RemoteRepositoryJreHttpClient("gradle-plugins", URI.create("https://plugins.gradle.org"));
        // lifecycleManager.add(repository);
        // arser.addRepository(repository);
        // publicRepositories.add(repository);
        //
        // repository = new VirtualRepository("public", publicRepositories);
        // lifecycleManager.add(repository);
        // arser.addRepository(repository);
        //
        // repository = new FileRepository("deploy-snapshots", Path.of(System.getProperty("java.io.tmpdir"), "arser", "deploy-snapshots").toUri(), true);
        // lifecycleManager.add(repository);
        // arser.addRepository(repository);
        //
        // repository = new VirtualRepository("public-snapshots", List.of(repository));
        // lifecycleManager.add(repository);
        // arser.addRepository(repository);

        // final JreHttpClientComponent httpClientComponent = new JreHttpClientComponent(arserConfig.getHttpClientConfig());
        // lifecycleManager.add(httpClientComponent);

        // FileRepository
        arserConfig.getRepositoryConfigsFile().forEach(config -> {
            final Repository repository = new FileRepository(config.getContextRoot(), config.getUri(), config.isWriteable());
            lifecycleManager.add(repository);

            arser.addRepository(repository);
        });

        // RemoteRepository
        arserConfig.getRepositoryConfigsRemote().forEach(config -> {
            final Repository repository = new RemoteRepositoryJreHttpClient(config.getContextRoot(), config.getUri());

            // if (config.getStoreConfig() instanceof FileStoreConfig fsc) {
            //     final BlobStoreComponent blobStoreComponent = new BlobStoreComponent(new FileBlobStore(fsc.getBaseUri()));
            //     lifecycleManager.add(blobStoreComponent);
            //
            //     repository = new CachedRepository(repository, blobStoreComponent.getBlobStore());
            //     lifecycleManager.add(repository);
            // }
            // else if (config.getStoreConfig() instanceof DatabaseStoreConfig dbsc) {
            //     final DatasourceComponent datasourceComponent = new DatasourceComponent(dbsc);
            //     lifecycleManager.add(datasourceComponent);
            //
            //     final BlobStoreComponent blobStoreComponent = new BlobStoreComponent(new JdbcBlobStore(datasourceComponent::getDataSource));
            //     lifecycleManager.add(blobStoreComponent);
            //
            //     repository = new CachedRepository(repository, blobStoreComponent.getBlobStore());
            //     lifecycleManager.add(repository);
            // }
            // else {
            lifecycleManager.add(repository);
            // }

            arser.addRepository(repository);
        });

        // VirtualRepository
        arserConfig.getRepositoryConfigsVirtual().forEach(config -> {
            final List<Repository> repositories = config.getRepositoryRefs().stream().map(arser::getRepository).toList();

            final Repository repository = new VirtualRepository(config.getContextRoot(), repositories);
            lifecycleManager.add(repository);

            arser.addRepository(repository);
        });

        // Server at last
        final JreHttpServer proxyServer = new JreHttpServer(arser, arserConfig.getServerConfig());
        // final JreHttpServer proxyServer = new JreHttpServer(arser, ServerConfig.builder()
        //         .threadNamePattern("http-server-%d")
        //         .threadPoolCoreSize(2)
        //         .threadPoolMaxSize(6)
        //         .port(7999)
        //         .build()
        // );
        lifecycleManager.add(proxyServer);

        return lifecycleManager;
    }

    private ArserJreServerApplication() {
        super();
    }
}
