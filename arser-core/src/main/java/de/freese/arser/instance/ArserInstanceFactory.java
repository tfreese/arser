// Created: 11 Mai 2025
package de.freese.arser.instance;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.config.ArserConfig;
import de.freese.arser.config.FileRepositoryConfig;
import de.freese.arser.config.RemoteRepositoryConfig;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.config.ThreadPoolConfig;
import de.freese.arser.config.VirtualRepositoryConfig;
import de.freese.arser.config.xml.ArserSettings;
import de.freese.arser.config.xml.ServerSetting;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.RemoteRepositoryJreHttpClient;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;

/**
 * Inspired by HazelcastInstanceFactory.
 *
 * @author Thomas Freese
 */
public final class ArserInstanceFactory {
    private static final AtomicInteger FACTORY_ID_GEN = new AtomicInteger(0);
    private static final ConcurrentMap<String, ArserInstance> INSTANCE_MAP = new ConcurrentHashMap<>(5);
    private static final Logger LOGGER = LoggerFactory.getLogger(ArserInstanceFactory.class);

    public static ArserInstance createArserInstance(final ArserConfig config) {
        final String name = createInstanceName(config);

        final LifecycleManager lifecycleManager = new LifecycleManager();

        final ArserInstance instance = new DefaultArserInstance(name, config, lifecycleManager);

        if (INSTANCE_MAP.putIfAbsent(name, instance) != null) {
            throw new IllegalStateException("ArserInstance with name '" + name + "' already exists!");
        }

        try {
            Files.createDirectories(config.getWorkingDir());
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        // FileRepository
        config.getRepositoryConfigsFile().forEach(repositoryConfig -> {
            final Repository repository = new FileRepository(repositoryConfig.getContextRoot(), repositoryConfig.getUri(), repositoryConfig.isWriteable());
            lifecycleManager.add(repository);

            instance.addRepository(repository);
        });

        // RemoteRepository
        config.getRepositoryConfigsRemote().forEach(repositoryConfig -> {
            final Repository repository = new RemoteRepositoryJreHttpClient(repositoryConfig.getContextRoot(),
                    repositoryConfig.getUri(),
                    config.getWorkingDir().resolve(repositoryConfig.getContextRoot()));
            lifecycleManager.add(repository);

            instance.addRepository(repository);
        });

        // VirtualRepository
        config.getRepositoryConfigsVirtual().forEach(repositoryConfig -> {
            final List<Repository> repositories = repositoryConfig.getRepositoryRefs().stream().map(instance::getRepository).toList();

            final Repository repository = new VirtualRepository(repositoryConfig.getContextRoot(), repositories);
            lifecycleManager.add(repository);

            instance.addRepository(repository);
        });

        try {
            LOGGER.info("starting {}", name);

            lifecycleManager.start();
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return instance;
    }

    public static ArserInstance createArserInstanceFromXml(final InputStream inputStreamSchema, final InputStream inputStreamXml) throws Exception {
        final Source schemaFile = new StreamSource(inputStreamSchema);

        final Source xmlFile = new StreamSource(inputStreamXml);

        // Validate Schema.
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        final Schema schema = schemaFactory.newSchema(schemaFile);

        // Validation either by Validator or Unmarshaller, otherwise the Stream is closed when parsing.
        // final Validator validator = schema.newValidator();
        // validator.validate(xmlFile);

        final JAXBContext jaxbContext = JAXBContext.newInstance(ArserSettings.class.getPackageName());
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(schema);

        final ArserSettings settings = (ArserSettings) unmarshaller.unmarshal(xmlFile);

        final ArserConfig.Builder arserConfigBuilder = ArserConfig.builder()
                .workingDir(Path.of(URI.create(settings.getWorkingDir())));

        final ServerSetting serverSetting = settings.getServerSetting();
        arserConfigBuilder.serverConfig(ServerConfig.builder()
                .port(serverSetting.getPort())
                .threadPoolConfig(ThreadPoolConfig.builder()
                        .namePattern(serverSetting.getThreadNamePattern())
                        .coreSize(serverSetting.getThreadPoolCoreSize())
                        .maxSize(serverSetting.getThreadPoolMaxSize())
                        .build())
                .build())
        ;

        // FileRepositories
        settings.getRepositories().getFiles().forEach(setting -> {
            final String contextRoot = setting.getContextRoot();

            arserConfigBuilder.addFileRepository(FileRepositoryConfig.builder()
                    .contextRoot(contextRoot)
                    .uri(URI.create(setting.getPath()))
                    .writeable(setting.isWriteable())
                    .build());
        });

        // RemoteRepositories
        settings.getRepositories().getRemotes().forEach(setting -> {
            final String contextRoot = setting.getContextRoot();

            arserConfigBuilder.addRemoteRepository(RemoteRepositoryConfig.builder()
                    .contextRoot(contextRoot)
                    .uri(URI.create(setting.getUri()))
                    .build());
        });

        // VirtualRepositories
        settings.getRepositories().getVirtuals().forEach(setting -> {
            final String contextRoot = setting.getContextRoot();

            arserConfigBuilder.addVirtualRepository(VirtualRepositoryConfig.builder()
                    .contextRoot(contextRoot)
                    .repositoryRefs(setting.getRepositoryReves())
                    .build());
        });

        return createArserInstance(arserConfigBuilder.build());
    }

    public static void shutdownAll() {
        final List<ArserInstance> instances = INSTANCE_MAP.values().stream()
                .sorted(Comparator.comparing(ArserInstance::getName))
                .toList();

        INSTANCE_MAP.clear();

        for (ArserInstance instance : instances) {
            try {
                instance.shutdown();
            }
            catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    private static String createInstanceName(final ArserConfig config) {
        return "arserInstance_" + FACTORY_ID_GEN.incrementAndGet();
    }

    private ArserInstanceFactory() {
        super();
    }
}
