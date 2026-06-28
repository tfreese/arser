// Created: 17.01.24
package de.freese.arser.model;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Thomas Freese
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

    private static Path toRelativePath(final URI uri) {
        String uriPath = uri.getPath();
        uriPath = uriPath.replace(' ', '_');

        if (uriPath.startsWith("/")) {
            uriPath = uriPath.substring(1);
        }

        return Path.of(uriPath);
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

    public Path toLocalPath(final URI baseUri) {
        final Path relativePath = toRelativePath(resource);

        return Path.of(baseUri).resolve(relativePath);
    }

    public URI toRemoteUri(final URI baseUri) {
        String pathResource = getResource().getPath();

        if (pathResource.startsWith("/")) {
            pathResource = pathResource.substring(1);
        }

        String newPath = baseUri.getPath();

        if (newPath.endsWith("/")) {
            newPath += pathResource;
        } else {
            newPath += "/" + pathResource;
        }

        return baseUri.resolve(newPath);

        // return new URI(
        //         baseUri.getScheme(),
        //         baseUri.getUserInfo(),
        //         baseUri.getHost(),
        //         baseUri.getPort(),
        //         newPath,
        //         baseUri.getQuery(),
        //         baseUri.getFragment()
        // );
    }

    @Override
    public String toString() {
        return getResource().toString();
    }
}
