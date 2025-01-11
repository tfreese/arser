// Created: 11 Jan. 2025
package de.freese.arser.core.response;

import java.net.URI;
import java.util.Objects;

/**
 * @author Thomas Freese
 */
public final class ResourceResponse {
    private final String repository;
    private final URI resourceUri;

    public ResourceResponse(final String repository, final URI resourceUri) {
        super();

        this.repository = Objects.requireNonNull(repository, "repository required");
        this.resourceUri = Objects.requireNonNull(resourceUri, "resourceUri required");
    }

    public String getRepository() {
        return repository;
    }

    /**
     * Schemes:
     * <ul>
     *     <li>file for local cached/stored resources</li>
     *     <li>http/https for remote resources</li>
     * </ul>
     */
    public URI getResourceUri() {
        return resourceUri;
    }
}
