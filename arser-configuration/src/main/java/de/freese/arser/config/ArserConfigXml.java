// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.config.xml.ArserSettings;
import de.freese.arser.config.xml.FileRepoSetting;
import de.freese.arser.config.xml.RemoteRepoSetting;
import de.freese.arser.config.xml.ServerSetting;
import de.freese.arser.config.xml.StoreSetting;
import de.freese.arser.config.xml.VirtualRepoSetting;

/**
 * @author Thomas Freese
 */
final class ArserConfigXml implements ArserConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArserConfigXml.class);

    public static final class Builder {
        // private HttpClientConfig httpClientConfig;
        private Path workingDir;

        private Builder() {
            super();
        }

        public Builder workingDir(final Path workingDir) {
            this.workingDir = workingDir;

            return this;
        }

        // public Builder httpClientConfig(final HttpClientConfig httpClientConfig) {
        //     this.httpClientConfig = httpClientConfig;
        //
        //     return this;
        // }

        ArserConfigXml build() {
            // ConfigValidator.value(httpClientConfig, Objects::nonNull, () -> "httpClientConfig required");
            ConfigValidator.value(workingDir, Objects::nonNull, () -> "workingDir required");

            return new ArserConfigXml(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    static ArserConfigXml fromXml(final String[] args) throws Exception {
        final URI configUri = findConfigFile(args);
        LOGGER.info("load settings from {}", configUri);

        final ArserConfigXml arserConfig;

        try (InputStream inputStream = configUri.toURL().openStream()) {
            arserConfig = fromXml(inputStream);
        }

        return arserConfig;
    }

    static ArserConfigXml fromXml(final InputStream inputStream) throws Exception {
        final URL url = ClassLoader.getSystemResource("xsd/arser-config.xsd");
        final Source schemaFile = new StreamSource(url.openStream());

        final Source xmlFile = new StreamSource(inputStream);

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

        return new ArserConfigXml(settings);
    }

    private static URI findConfigFile(final String[] args) throws Exception {
        LOGGER.info("Try to find arser-config.xml");

        URI configUri = null;

        if (args != null && args.length == 2) {
            final String parameter = args[0];

            if ("-arser.config".equals(parameter)) {
                configUri = Path.of(args[1]).toUri();
            }
        }

        if (configUri == null) {
            final String propertyValue = System.getProperty("arser.config");

            if (propertyValue != null) {
                configUri = Path.of(propertyValue).toUri();
            }
        }

        if (configUri == null) {
            final String envValue = System.getenv("arser.config");

            if (envValue != null) {
                configUri = Path.of(envValue).toUri();
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

    // private final HttpClientConfig httpClientConfig;
    private final Map<String, RepositoryConfig> repositoryConfigs = new HashMap<>();
    private final ServerConfig serverConfig;
    private final Path workingDir;

    ArserConfigXml(final ArserSettings settings) {
        super();

        Objects.requireNonNull(settings, "settings required");

        workingDir = Path.of(URI.create(settings.getWorkingDir()));

        final ServerSetting serverSetting = settings.getServerSetting();
        serverConfig = ServerConfig.builder()
                .port(serverSetting.getPort())
                .threadNamePattern(serverSetting.getThreadNamePattern())
                .threadPoolCoreSize(serverSetting.getThreadPoolCoreSize())
                .threadPoolMaxSize(serverSetting.getThreadPoolMaxSize())
                .build();

        // final HttpClientSetting httpClientSetting = settings.getHttpClientSetting();
        // httpClientConfig = HttpClientConfig.builder()
        //         .threadNamePattern(httpClientSetting.getThreadNamePattern())
        //         .threadPoolCoreSize(httpClientSetting.getThreadPoolCoreSize())
        //         .threadPoolMaxSize(httpClientSetting.getThreadPoolMaxSize())
        //         .build();

        configureFileRepositories(settings.getRepositories().getFiles());
        configureRemoteRepositories(settings.getRepositories().getRemotes());
        configureVirtualRepositories(settings.getRepositories().getVirtuals());
    }

    private ArserConfigXml(final Builder builder) {
        super();

        // httpClientConfig = builder.httpClientConfig;
        serverConfig = null;
        workingDir = builder.workingDir;
    }

    // public HttpClientConfig getHttpClientConfig() {
    //     return httpClientConfig;
    // }

    @Override
    public List<FileRepositoryConfig> getRepositoryConfigsFile() {
        return repositoryConfigs.values().stream()
                .filter(config -> config.getClass().isAssignableFrom(FileRepositoryConfig.class))
                .map(FileRepositoryConfig.class::cast)
                .toList()
                ;
    }

    @Override
    public List<RemoteRepositoryConfig> getRepositoryConfigsRemote() {
        return repositoryConfigs.values().stream()
                .filter(config -> config.getClass().isAssignableFrom(RemoteRepositoryConfig.class))
                .map(RemoteRepositoryConfig.class::cast)
                .toList()
                ;
    }

    @Override
    public List<VirtualRepositoryConfig> getRepositoryConfigsVirtual() {
        return repositoryConfigs.values().stream()
                .filter(config -> config.getClass().isAssignableFrom(VirtualRepositoryConfig.class))
                .map(VirtualRepositoryConfig.class::cast)
                .toList()
                ;
    }

    @Override
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    @Override
    public Path getWorkingDir() {
        return workingDir;
    }

    private void configureFileRepositories(final List<FileRepoSetting> settings) {
        settings.forEach(setting -> {
            final String contextRoot = setting.getContextRoot();

            if (repositoryConfigs.containsKey(contextRoot)) {
                throw new IllegalArgumentException("repository already exist: " + contextRoot);
            }

            repositoryConfigs.put(contextRoot, FileRepositoryConfig.builder()
                    .contextRoot(contextRoot)
                    .uri(URI.create(setting.getPath()))
                    .writeable(setting.isWriteable())
                    .build()
            );
        });
    }

    private void configureRemoteRepositories(final List<RemoteRepoSetting> settings) {
        settings.forEach(setting -> {
            final String contextRoot = setting.getContextRoot();

            if (repositoryConfigs.containsKey(contextRoot)) {
                throw new IllegalArgumentException("repository already exist: " + contextRoot);
            }

            final StoreSetting storeSetting = setting.getStoreSetting();
            StoreConfig storeConfig = null;

            if (storeSetting != null) {
                if ("file".equalsIgnoreCase(storeSetting.getType())) {
                    storeConfig = FileStoreConfig.builder()
                            .uri(URI.create(storeSetting.getUri()))
                            .build()
                    ;
                }
                else if ("jdbc".equalsIgnoreCase(storeSetting.getType())) {
                    storeConfig = DatabaseStoreConfig.builder()
                            .uri(URI.create(storeSetting.getUri()))
                            .driverClassName(storeSetting.getDriverClassName())
                            .user(storeSetting.getUser())
                            .password(storeSetting.getPassword())
                            .poolCoreSize(storeSetting.getPoolCoreSize())
                            .poolMaxSize(storeSetting.getPoolMaxSize())
                            .poolName(contextRoot)
                            .build()
                    ;
                }
                else {
                    throw new IllegalArgumentException("unsupported store type: " + storeSetting.getType());
                }
            }

            repositoryConfigs.put(contextRoot, RemoteRepositoryConfig.builder()
                    .contextRoot(contextRoot)
                    .uri(URI.create(setting.getUri()))
                    .storeConfig(storeConfig)
                    .build()
            );
        });
    }

    private void configureVirtualRepositories(final List<VirtualRepoSetting> settings) {
        settings.forEach(setting -> {
            final String contextRoot = setting.getContextRoot();

            if (repositoryConfigs.containsKey(contextRoot)) {
                throw new IllegalArgumentException("repository already exist: " + contextRoot);
            }

            final List<String> repositoryRefs = setting.getRepositoryReves();

            repositoryRefs.forEach(ref -> {
                if (!repositoryConfigs.containsKey(ref)) {
                    throw new IllegalArgumentException("repository reference not exist: " + ref);
                }
            });

            repositoryConfigs.put(contextRoot, VirtualRepositoryConfig.builder()
                    .contextRoot(contextRoot)
                    .repositoryRefs(repositoryRefs)
                    .build()
            );
        });
    }
}
