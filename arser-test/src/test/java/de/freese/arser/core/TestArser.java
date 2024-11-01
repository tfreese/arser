// Created: 18.01.24
package de.freese.arser.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.config.ArserConfig;
import de.freese.arser.core.config.HttpClientConfig;
import de.freese.arser.core.config.LocalRepositoryConfig;
import de.freese.arser.core.config.RemoteRepositoryConfig;
import de.freese.arser.core.config.VirtualRepositoryConfig;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.JreHttpClientRemoteRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;
import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestArser {
    private static final Path PATH_TEST = Paths.get(System.getProperty("java.io.tmpdir"), "arser-test");

    private static LifecycleManager lifecycleManager;
    private static Repository repositoryGradleReleases;
    private static Repository repositoryLocal;
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

        final ArserConfig arserConfig = ArserConfig.builder()
                .httpClientConfig(HttpClientConfig.builder()
                        .threadNamePattern("test-http-%d")
                        .threadPoolCoreSize(2)
                        .threadPoolMaxSize(4)
                        .build()
                )
                .workingDir(PATH_TEST)
                .build();

        final JreHttpClientComponent httpClientComponent = new JreHttpClientComponent(arserConfig.getHttpClientConfig());
        lifecycleManager.add(httpClientComponent);

        repositoryMavenCentral = new JreHttpClientRemoteRepository(RemoteRepositoryConfig.builder()
                .name("maven-central")
                .uri(URI.create("https://repo1.maven.org/maven2"))
                .build(),
                httpClientComponent::getHttpClient, arserConfig.getWorkingDir());
        lifecycleManager.add(repositoryMavenCentral);

        repositoryGradleReleases = new JreHttpClientRemoteRepository(RemoteRepositoryConfig.builder()
                .name("gradle-releases")
                .uri(URI.create("https://repo.gradle.org/gradle/libs-releases"))
                .build(),
                httpClientComponent::getHttpClient, arserConfig.getWorkingDir());
        lifecycleManager.add(repositoryGradleReleases);

        repositoryLocal = new FileRepository(LocalRepositoryConfig.builder()
                .name("deploy-snapshots")
                .uri(PATH_TEST.resolve("deploy-snapshots").toUri())
                .writeable(true)
                .build()
        );
        lifecycleManager.add(repositoryLocal);

        repositoryVirtual = new VirtualRepository(VirtualRepositoryConfig.builder()
                .name("public")
                .repositories(List.of(repositoryMavenCentral, repositoryGradleReleases, repositoryLocal))
                .build()
        );
        lifecycleManager.add(repositoryVirtual);

        lifecycleManager.start();
    }

    @Test
    void testLocalWriteable() throws Exception {
        final Arser arser = new Arser();
        arser.addRepository(repositoryLocal);

        final URI resource = URI.create("/deploy-snapshots/org/test/0.0.1/test-0.0.1.pom");
        final ResourceRequest resourceRequest = ResourceRequest.of(resource);

        final byte[] buffer = "Test-Pom".getBytes(StandardCharsets.UTF_8);

        try (InputStream inputStream = new ByteArrayInputStream(buffer)) {
            arser.write(resourceRequest, inputStream);
        }

        final URI fileRelativeResourceUri = URI.create(resource.getPath().substring(1));
        final URI fileAbsoluteUri = repositoryLocal.getUri().resolve(fileRelativeResourceUri);
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
        boolean exist1 = arser.exist(resourceRequest);
        assertTrue(exist1);

        resourceRequest = ResourceRequest.of(URI.create("/public/org/gradle/gradle-tooling-api/8.2.1/gradle-tooling-api-8.2.1.pom"));
        boolean exist2 = arser.exist(resourceRequest);
        assertTrue(exist2);

        assertEquals(exist1, exist2);

        // Only in https://repo1.maven.org/maven2
        resourceRequest = ResourceRequest.of(URI.create("/public/org/apache/maven/maven/3.8.4/maven-3.8.4.pom"));
        exist1 = arser.exist(resourceRequest);
        assertTrue(exist1);

        resourceRequest = ResourceRequest.of(URI.create("/public/org/apache/maven/maven/3.8.4/maven-3.8.4.pom"));
        exist2 = arser.exist(resourceRequest);
        assertTrue(exist2);

        assertEquals(exist1, exist2);
    }
}
