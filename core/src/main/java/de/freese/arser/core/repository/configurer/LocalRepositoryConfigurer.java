// Created: 14.01.24
package de.freese.arser.core.repository.configurer;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public interface LocalRepositoryConfigurer extends RepositoryConfigurer {
    void setUri(URI uri);

    void setWriteable(boolean writeable);
}
