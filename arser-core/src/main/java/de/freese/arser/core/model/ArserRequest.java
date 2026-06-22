// Created: 17.01.24
package de.freese.arser.core.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Thomas Freese
 */
public final class ArserRequest {
    /**
     * /public/org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom
     */
    public static ArserRequest of(final URI requestUri) {
        return of(requestUri.getPath());
    }

    /**
     * /public/org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom
     */
    public static ArserRequest of(final String requestPath) {
        String path = requestPath;

        // Strip Leading '/'.
        path = path.substring(1);

        // Get the first Element.
        final String contextRoot = path.substring(0, path.indexOf('/'));

        // Rest of the Path.
        final String resourcePath = path.substring(contextRoot.length());
        final URI resource = URI.create(resourcePath);

        final List<String> splits = new ArrayList<>(Arrays.asList(resourcePath.split("/")));

        // Remove first if empty.
        if (splits.getFirst().isEmpty()) {
            splits.removeFirst();
        }

        // Remove File.
        if (splits.getLast().contains(".")) {
            splits.removeLast();
        }

        final String version = splits.removeLast();
        final String artifactId = splits.removeLast();
        final String groupId = String.join(".", splits);

        return new ArserRequest(resource, groupId, artifactId, version);
    }

    private final String artifactId;
    private final String groupId;
    private final URI resource;
    private final String version;

    private ArserRequest(final URI resource, final String groupId, final String artifactId, final String version) {
        super();

        this.resource = resource;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    /**
     * Default: groupId + ":" + artifactId + ":" + version
     */
    public String getId() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }

    public URI getResource() {
        return resource;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return getResource().getPath();
    }
}
