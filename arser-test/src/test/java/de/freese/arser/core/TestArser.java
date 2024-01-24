// Created: 18.01.24
package de.freese.arser.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.local.FileRepository;
import de.freese.arser.core.repository.virtual.VirtualRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.settings.ArserSettings;
import de.freese.arser.repository.remote.JreHttpClientRemoteRepository;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestArser {
    private static final Path PATH_TEST = Paths.get(System.getProperty("java.io.tmpdir"), "arser-test", "requestHandler");

    private static JreHttpClientComponent httpClientComponent;
    private static LifecycleManager lifecycleManager;
    private static Repository repositoryGradleReleases;
    private static Repository repositoryLocalWriteable;
    private static Repository repositoryMavenCentral;
    private static Repository repositoryVirtual;

    @AfterAll
    static void afterAll() throws Exception {
        if (lifecycleManager != null) {
            lifecycleManager.stop();
        }

        Files.walkFileTree(PATH_TEST, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        lifecycleManager = new LifecycleManager();

        final ArserSettings arserSettings = ArserSettings.fromEmpty();

        arserSettings.getHttpClientConfig().setThreadNamePattern("test-http-%d");
        arserSettings.getHttpClientConfig().setThreadPoolCoreSize(2);
        arserSettings.getHttpClientConfig().setThreadPoolMaxSize(4);

        httpClientComponent = new JreHttpClientComponent(arserSettings.getHttpClientConfig());
        lifecycleManager.add(httpClientComponent);

        repositoryMavenCentral = new JreHttpClientRemoteRepository("maven-central", URI.create("https://repo1.maven.org/maven2"), httpClientComponent::getHttpClient);
        lifecycleManager.add(repositoryMavenCentral);

        repositoryGradleReleases = new JreHttpClientRemoteRepository("gradle-releases", URI.create("https://repo.gradle.org/gradle/libs-releases"),
                httpClientComponent::getHttpClient);
        lifecycleManager.add(repositoryGradleReleases);

        repositoryLocalWriteable = new FileRepository("deploy-snapshots", PATH_TEST.resolve("deploy-snapshots").toUri(), true);
        lifecycleManager.add(repositoryLocalWriteable);

        repositoryVirtual = new VirtualRepository("public");
        ((VirtualRepository) repositoryVirtual).add(repositoryMavenCentral);
        ((VirtualRepository) repositoryVirtual).add(repositoryGradleReleases);
        ((VirtualRepository) repositoryVirtual).add(repositoryLocalWriteable);
        lifecycleManager.add(repositoryVirtual);

        lifecycleManager.start();
    }

    @Test
    void testLocalWriteable() throws Exception {
        final Arser arser = new Arser();
        arser.addRepository(repositoryLocalWriteable);

        final URI resource = URI.create("/" + repositoryLocalWriteable.getName() + "/org/test/0.0.1/test-0.0.1.pom");
        final ResourceRequest resourceRequest = ResourceRequest.of(resource);

        final byte[] buf = "Test-Pom".getBytes(StandardCharsets.UTF_8);

        try (InputStream inputStream = new ByteArrayInputStream(buf)) {
            arser.write(resourceRequest, inputStream);
        }

        final URI fileRelativeResourceUri = URI.create(resource.getPath().substring(1));
        final URI fileAbsoluteUri = repositoryLocalWriteable.getUri().resolve(fileRelativeResourceUri);
        final Path path = Paths.get(fileAbsoluteUri);
        assertTrue(Files.exists(path));
        assertTrue(Files.isReadable(path));
        assertTrue(Files.size(path) > 1L);
    }

    @Test
    void testRemoteGradleReleases() throws Exception {
        final Arser arser = new Arser();
        arser.addRepository(repositoryGradleReleases);

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/gradle-releases/org/gradle/gradle-tooling-api/8.2.1/gradle-tooling-api-8.2.1.pom"));
        final boolean exist = arser.exist(resourceRequest);

        assertTrue(exist);
    }

    @Test
    void testRemoteMavenCentral() throws Exception {
        final Arser arser = new Arser();
        arser.addRepository(repositoryMavenCentral);

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/maven-central/org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom"));
        final boolean exist = arser.exist(resourceRequest);

        assertTrue(exist);
    }

    @Test
    void testVirtual() throws Exception {
        final Arser arser = new Arser();
        arser.addRepository(repositoryVirtual);

        // Only in https://repo.gradle.org/gradle/libs-releases
        ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/public/org/gradle/gradle-tooling-api/8.2.1/gradle-tooling-api-8.2.1.pom"));
        boolean exist = arser.exist(resourceRequest);
        assertTrue(exist);

        // Only in https://repo1.maven.org/maven2
        resourceRequest = ResourceRequest.of(URI.create("/public/org/apache/maven/maven/3.8.4/maven-3.8.4.pom"));
        exist = arser.exist(resourceRequest);
        assertTrue(exist);
    }
}
