// Created: 11 Mai 2025
package de.freese.arser.instance;

import java.util.function.Consumer;

import de.freese.arser.config.ArserConfig;
import de.freese.arser.core.repository.Repository;

/**
 * @author Thomas Freese
 */
public interface ArserInstance {
    void addRepository(Repository repository);

    void forEach(Consumer<Repository> consumer);

    ArserConfig getConfig();

    String getName();

    Repository getRepository(String contextRoot);

    int getRepositoryCount();

    void shutdown();
}
