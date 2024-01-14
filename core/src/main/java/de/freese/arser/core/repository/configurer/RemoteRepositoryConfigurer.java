// Created: 14.01.24
package de.freese.arser.core.repository.configurer;

import java.net.URI;

import de.freese.arser.config.StoreConfig;

/**
 * @author Thomas Freese
 */
public interface RemoteRepositoryConfigurer extends RepositoryConfigurer {
    void setStoreConfig(StoreConfig storeConfig);

    void setUri(URI uri);
}
