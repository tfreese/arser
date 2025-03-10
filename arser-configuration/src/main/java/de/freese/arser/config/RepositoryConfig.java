// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public interface RepositoryConfig {
    String getContextRoot();

    URI getUri();
}
