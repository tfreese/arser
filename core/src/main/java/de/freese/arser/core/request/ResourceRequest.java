// Created: 17.01.24
package de.freese.arser.core.request;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Thomas Freese
 */
public final class ResourceRequest {
    public static ResourceRequest of(final URI requestUri) {
        String path = requestUri.getPath();

        // Strip Leading /
        // /public/org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom
        path = path.substring(1);

        // Get first Element
        final String contextRoot = path.substring(0, path.indexOf('/'));

        // Rest of Path
        // /org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom
        final String resourcePath = path.substring(contextRoot.length());
        final URI resource = URI.create(resourcePath);

        final List<String> splits = new ArrayList<>(Arrays.asList(resourcePath.split("/")));

        // Remove first if empty.
        if (splits.getFirst().isEmpty()) {
            splits.removeFirst();
        }

        // Remove File
        if (splits.getLast().contains(".")) {
            splits.removeLast();
        }

        final String version = splits.removeLast();
        final String artifactId = splits.removeLast();
        final String groupId = String.join(".", splits);

        return new ResourceRequest(contextRoot, resource, groupId, artifactId, version);
    }

    private final String artifactId;
    private final String contextRoot;
    private final String groupId;
    private final URI resource;
    private final String version;

    private ResourceRequest(final String contextRoot, final URI resource, final String groupId, final String artifactId, final String version) {
        super();

        this.contextRoot = contextRoot;
        this.resource = resource;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getContextRoot() {
        return contextRoot;
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
}
