package de.freese.arser.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import de.freese.arser.config.ServerConfig;
import de.freese.arser.repository.file.FileRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.virtual.VirtualRepositoryConfig;

/**
 * @author Thomas Freese
 * @since 04.07.26
 */
@JsonDeserialize(builder = ArserConfig.ArserConfigBuilder.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonRootName("arserConfig")
@JsonPropertyOrder(value = {"serverConfig", "fileRepositoryConfigs", "httpRepositoryConfigs", "virtualRepositoryConfigs"})
public final class ArserConfig {

    @JsonPOJOBuilder(withPrefix = "")
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.NONE)
    public static final class ArserConfigBuilder {
        private final List<FileRepositoryConfig> fileRepositoryConfigs = new ArrayList<>();
        private final List<HttpRepositoryConfig> httpRepositoryConfigs = new ArrayList<>();
        private final List<VirtualRepositoryConfig> virtualRepositoryConfigs = new ArrayList<>();
        private ServerConfig serverConfig;

        public ArserConfigBuilder addFileRepositoryConfig(final FileRepositoryConfig fileRepositoryConfig) {
            this.fileRepositoryConfigs.add(fileRepositoryConfig);

            return this;
        }

        public ArserConfigBuilder addHttpRepositoryConfig(final HttpRepositoryConfig httpRepositoryConfig) {
            this.httpRepositoryConfigs.add(httpRepositoryConfig);

            return this;
        }

        public ArserConfigBuilder addVirtualRepositoryConfig(final VirtualRepositoryConfig virtualRepositoryConfig) {
            this.virtualRepositoryConfigs.add(virtualRepositoryConfig);

            return this;
        }

        public ArserConfig build() {
            Objects.requireNonNull(serverConfig, "serverConfig required");

            return new ArserConfig(this);
        }

        @JsonSetter("fileRepositoryConfigs")
        public ArserConfigBuilder fileRepositoryConfigs(final List<FileRepositoryConfig> fileRepositoryConfigs) {
            this.fileRepositoryConfigs.clear();
            this.fileRepositoryConfigs.addAll(fileRepositoryConfigs);

            return this;
        }

        @JsonSetter("httpRepositoryConfigs")
        public ArserConfigBuilder httpRepositoryConfigs(final List<HttpRepositoryConfig> httpRepositoryConfigs) {
            this.httpRepositoryConfigs.clear();
            this.httpRepositoryConfigs.addAll(httpRepositoryConfigs);

            return this;
        }

        public ArserConfigBuilder serverConfig(final ServerConfig serverConfig) {
            this.serverConfig = serverConfig;

            return this;
        }

        @JsonSetter("virtualRepositoryConfigs")
        public ArserConfigBuilder virtualRepositoryConfigs(final List<VirtualRepositoryConfig> virtualRepositoryConfigs) {
            this.virtualRepositoryConfigs.clear();
            this.virtualRepositoryConfigs.addAll(virtualRepositoryConfigs);

            return this;
        }
    }

    public static ArserConfigBuilder builder() {
        return new ArserConfigBuilder();
    }

    private final List<FileRepositoryConfig> fileRepositoryConfigs;
    private final List<HttpRepositoryConfig> httpRepositoryConfigs;
    private final ServerConfig serverConfig;
    private final List<VirtualRepositoryConfig> virtualRepositoryConfigs;

    private ArserConfig(final ArserConfigBuilder builder) {
        super();

        this.fileRepositoryConfigs = List.copyOf(builder.fileRepositoryConfigs);
        this.httpRepositoryConfigs = List.copyOf(builder.httpRepositoryConfigs);
        this.virtualRepositoryConfigs = List.copyOf(builder.virtualRepositoryConfigs);
        this.serverConfig = builder.serverConfig;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ArserConfig that = (ArserConfig) o;

        return Objects.equals(fileRepositoryConfigs, that.fileRepositoryConfigs)
                && Objects.equals(httpRepositoryConfigs, that.httpRepositoryConfigs)
                && Objects.equals(serverConfig, that.serverConfig)
                && Objects.equals(virtualRepositoryConfigs, that.virtualRepositoryConfigs);
    }

    @JacksonXmlElementWrapper(localName = "fileRepositoryConfigs")
    @JacksonXmlProperty(localName = "fileRepositoryConfig")
    public List<FileRepositoryConfig> fileRepositoryConfigs() {
        return fileRepositoryConfigs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileRepositoryConfigs, httpRepositoryConfigs, serverConfig, virtualRepositoryConfigs);
    }

    @JacksonXmlElementWrapper(localName = "httpRepositoryConfigs")
    @JacksonXmlProperty(localName = "httpRepositoryConfig")
    public List<HttpRepositoryConfig> httpRepositoryConfigs() {
        return httpRepositoryConfigs;
    }

    public ServerConfig serverConfig() {
        return serverConfig;
    }

    @JacksonXmlElementWrapper(localName = "virtualRepositoryConfigs")
    @JacksonXmlProperty(localName = "virtualRepositoryConfig")
    public List<VirtualRepositoryConfig> virtualRepositoryConfigs() {
        return virtualRepositoryConfigs;
    }
    // @Override
    // public Arser build(final LifeCycleRegistry lifeCycleRegistry) throws Exception {
    //     if (repositoryBuilders.isEmpty()) {
    //         throw new IllegalStateException("No repository builders defined");
    //     }
    //
    //     final Map<String, Repository> repositoryMap = new HashMap<>();
    //
    //     for (final RepositoryBuilder<?, ?> repositoryBuilder : repositoryBuilders) {
    //         final Repository repository = repositoryBuilder.build(lifeCycleRegistry);
    //
    //         if (repositoryMap.containsKey(repository.getName())) {
    //             throw new IllegalStateException("Repository already exists: " + repository.getName());
    //         }
    //
    //         repositoryMap.put(repository.getName(), repository);
    //     }
    //
    //     // VirtualRepositories
    //     for (final VirtualRepositoryBuilder virtualRepositoryBuilder : virtualRepositoryBuilders) {
    //         virtualRepositoryBuilder.repositoryProvider(repositoryMap::get);
    //
    //         if (repositoryMap.containsKey(virtualRepositoryBuilder.getName())) {
    //             throw new IllegalStateException("Repository already exists: " + virtualRepositoryBuilder.getName());
    //         }
    //
    //         final Repository repository = virtualRepositoryBuilder.build(lifeCycleRegistry);
    //         repositoryMap.put(repository.getName(), repository);
    //     }
    //
    //     return new DefaultArser(repositoryMap);
    // }
}
