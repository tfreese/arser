// Created: 14.01.24
package de.freese.arser.core.repository.configurer;

import java.util.List;

/**
 * @author Thomas Freese
 */
public interface VirtualRepositoryConfigurer extends RepositoryConfigurer {
    void setRepositoryNames(List<String> repositoryNames);
}
