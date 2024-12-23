// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
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

import de.freese.arser.config.xml.ArserSettings;
import de.freese.arser.config.xml.FileRepoSetting;
import de.freese.arser.config.xml.RemoteRepoSetting;
import de.freese.arser.config.xml.ServerSetting;
import de.freese.arser.config.xml.StoreSetting;
import de.freese.arser.config.xml.VirtualRepoSetting;

/**
 * @author Thomas Freese
 */
public final class ArserConfig {
    public static final class Builder {
        private HttpClientConfig httpClientConfig;
        private Path workingDir;

        private Builder() {
            super();
        }

        public ArserConfig build() {
            ConfigValidator.value(httpClientConfig, Objects::nonNull, () -> "httpClientConfig required");
            ConfigValidator.value(workingDir, Objects::nonNull, () -> "workingDir required");

            return new ArserConfig(this);
        }

        public Builder httpClientConfig(final HttpClientConfig httpClientConfig) {
            this.httpClientConfig = httpClientConfig;

            return this;
        }

        public Builder workingDir(final Path workingDir) {
            this.workingDir = workingDir;

            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ArserConfig fromXml(final InputStream inputStream) throws Exception {
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

        return new ArserConfig(settings);
    }

    // private final HttpClientConfig httpClientConfig;
    private final Map<String, RepositoryConfig> repositoryConfigs = new HashMap<>();
    private final ServerConfig serverConfig;
    private final Path workingDir;

    private ArserConfig(final ArserSettings settings) {
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

    private ArserConfig(final Builder builder) {
        super();

        // httpClientConfig = builder.httpClientConfig;
        serverConfig = null;
        workingDir = builder.workingDir;
    }

    // public HttpClientConfig getHttpClientConfig() {
    //     return httpClientConfig;
    // }

    public List<FileRepositoryConfig> getRepositoryConfigsLocal() {
        return repositoryConfigs.values().stream()
                .filter(config -> config.getClass().isAssignableFrom(FileRepositoryConfig.class))
                .map(FileRepositoryConfig.class::cast)
                .toList()
                ;
    }

    public List<RemoteRepositoryConfig> getRepositoryConfigsRemote() {
        return repositoryConfigs.values().stream()
                .filter(config -> config.getClass().isAssignableFrom(RemoteRepositoryConfig.class))
                .map(RemoteRepositoryConfig.class::cast)
                .toList()
                ;
    }

    public List<VirtualRepositoryConfig> getRepositoryConfigsVirtual() {
        return repositoryConfigs.values().stream()
                .filter(config -> config.getClass().isAssignableFrom(VirtualRepositoryConfig.class))
                .map(VirtualRepositoryConfig.class::cast)
                .toList()
                ;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public Path getTempDir() {
        return workingDir.resolve("temp");
    }

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
