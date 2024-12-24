// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Thomas Freese
 */
public interface ArserConfig {
    static ArserConfig fromXml(final String[] args) throws Exception {
        return ArserConfigXml.fromXml(args);
    }

    static ArserConfig fromXml(final InputStream inputStream) throws Exception {
        return ArserConfigXml.fromXml(inputStream);
    }

    // HttpClientConfig getHttpClientConfig();

    List<FileRepositoryConfig> getRepositoryConfigsFile();

    List<RemoteRepositoryConfig> getRepositoryConfigsRemote();

    List<VirtualRepositoryConfig> getRepositoryConfigsVirtual();

    ServerConfig getServerConfig();

    default Path getTempDir() {
        return getWorkingDir().resolve("temp");
    }

    Path getWorkingDir();
}
