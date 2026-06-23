// Created: 17.01.24
package de.freese.arser.core.model;

import java.net.URI;
import java.util.Objects;

/**
 * @author Thomas Freese
 */
public final class ArserWebRequest extends ArserRequest {
    /**
     * /public/org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom
     */
    public static ArserWebRequest of(final URI requestUri) {
        return of(requestUri.getPath());
    }

    /**
     * /public/org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom
     */
    public static ArserWebRequest of(final String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath required");

        String path = resourcePath;

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // Get the first Element.
        final String contextRoot = path.substring(0, path.indexOf('/'));

        // Rest of the Path.
        path = path.substring(contextRoot.length() + 1);

        final ArserRequest arserRequest = ArserRequest.of(path);

        return new ArserWebRequest(contextRoot, arserRequest.getResource(), arserRequest.getGroupId(), arserRequest.getArtifactId(), arserRequest.getVersion());
    }

    private final String contextRoot;

    private ArserWebRequest(final String contextRoot, final URI resource, final String groupId, final String artifactId, final String version) {
        super(resource, groupId, artifactId, version);

        this.contextRoot = Objects.requireNonNull(contextRoot, "contextRoot required");
    }

    /**
     * @return Used as Repositoryname.
     */
    public String getContextRoot() {
        return contextRoot;
    }
}
