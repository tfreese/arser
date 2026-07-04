package de.freese.arser.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Thomas Freese
 * @since 17.01.24
 */
public class ArserRequest {
    /**
     * org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom
     */
    public static ArserRequest of(final String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath required");

        final URI resource;

        if (resourcePath.startsWith("/")) {
            resource = URI.create(resourcePath.substring(1));
        } else {
            resource = URI.create(resourcePath);
        }

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

    public ArserRequest(final URI resource, final String groupId, final String artifactId, final String version) {
        super();

        this.resource = Objects.requireNonNull(resource, "resource required");
        this.groupId = Objects.requireNonNull(groupId, "groupId required");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId required");
        this.version = Objects.requireNonNull(version, "version required");
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
        return getResource().toString();
    }
}
