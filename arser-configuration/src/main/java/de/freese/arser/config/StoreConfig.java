// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public interface StoreConfig {
    enum StoreType {
        FILE,
        DATABASE
    }

    static DatabaseStoreConfig.Builder of() {
        return DatabaseStoreConfig.builder();
    }

    static FileStoreConfig.Builder ofFile() {
        return FileStoreConfig.builder();
    }

    URI getUri();
}
