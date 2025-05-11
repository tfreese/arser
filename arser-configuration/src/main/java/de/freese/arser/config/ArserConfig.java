// Created: 11 Mai 2025
package de.freese.arser.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public final class ArserConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArserConfig.class);

    public static final class Builder {
        private final List<FileRepositoryConfig> fileRepositoryConfigs = new ArrayList<>();
        private final List<RemoteRepositoryConfig> remoteRepositoryConfigs = new ArrayList<>();
        private final List<VirtualRepositoryConfig> virtualRepositoryConfigs = new ArrayList<>();
        private ServerConfig serverConfig;
        private Path workingDir;

        private Builder() {
            super();
        }

        public ArserConfig.Builder addFileRepository(final FileRepositoryConfig repositoryConfig) {
            fileRepositoryConfigs.add(repositoryConfig);

            return this;
        }

        public ArserConfig.Builder addRemoteRepository(final RemoteRepositoryConfig repositoryConfig) {
            remoteRepositoryConfigs.add(repositoryConfig);

            return this;
        }

        public ArserConfig.Builder addVirtualRepository(final VirtualRepositoryConfig repositoryConfig) {
            virtualRepositoryConfigs.add(repositoryConfig);

            return this;
        }

        public ArserConfig build() {
            // ConfigValidator.value(workingDir, Objects::nonNull, () -> "workingDir required");
            if (workingDir == null) {
                workingDir = Path.of(System.getProperty("java.io.tmpdir"), "arser");

                LOGGER.info("workingDir not set, using default: {}", workingDir);
            }

            return new ArserConfig(this);
        }

        public ArserConfig.Builder serverConfig(final ServerConfig serverConfig) {
            this.serverConfig = serverConfig;

            return this;
        }

        public ArserConfig.Builder workingDir(final Path workingDir) {
            this.workingDir = workingDir;

            return this;
        }
    }

    public static ArserConfig.Builder builder() {
        return new ArserConfig.Builder();
    }

    private final List<FileRepositoryConfig> fileRepositoryConfigs;
    private final List<RemoteRepositoryConfig> remoteRepositoryConfigs;
    private final ServerConfig serverConfig;
    private final List<VirtualRepositoryConfig> virtualRepositoryConfigs;
    private final Path workingDir;

    private ArserConfig(final Builder builder) {
        super();

        workingDir = builder.workingDir;
        serverConfig = builder.serverConfig;
        fileRepositoryConfigs = builder.fileRepositoryConfigs;
        remoteRepositoryConfigs = builder.remoteRepositoryConfigs;
        virtualRepositoryConfigs = builder.virtualRepositoryConfigs;
    }

    public List<FileRepositoryConfig> getRepositoryConfigsFile() {
        return fileRepositoryConfigs;
    }

    public List<RemoteRepositoryConfig> getRepositoryConfigsRemote() {
        return remoteRepositoryConfigs;
    }

    public List<VirtualRepositoryConfig> getRepositoryConfigsVirtual() {
        return virtualRepositoryConfigs;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public Path getWorkingDir() {
        return workingDir;
    }
}
