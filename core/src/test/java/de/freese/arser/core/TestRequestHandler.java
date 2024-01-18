// Created: 18.01.24
package de.freese.arser.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.freese.arser.config.ClientConfig;
import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.remote.JreHttpRemoteRepository;
import de.freese.arser.core.repository.virtual.DefaultVirtualRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.request.handler.DefaultRequestHandler;
import de.freese.arser.core.request.handler.RequestHandler;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestRequestHandler {
    private static JreHttpClientComponent httpClientComponent;
    private static LifecycleManager lifecycleManager;
    private static Repository repositoryGradleReleases;
    private static Repository repositoryMavenCentral;
    private static Repository repositoryVirtual;

    @AfterAll
    static void afterAll() throws Exception {
        if (lifecycleManager != null) {
            lifecycleManager.stop();
        }
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        lifecycleManager = new LifecycleManager();

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setThreadNamePattern("test-http-%d");
        clientConfig.setThreadPoolCoreSize(2);
        clientConfig.setThreadPoolMaxSize(4);

        httpClientComponent = new JreHttpClientComponent(clientConfig);
        lifecycleManager.add(httpClientComponent);

        repositoryMavenCentral = new JreHttpRemoteRepository("maven-central", URI.create("https://repo1.maven.org/maven2"), httpClientComponent::getHttpClient);
        lifecycleManager.add(repositoryMavenCentral);

        repositoryGradleReleases = new JreHttpRemoteRepository("gradle-releases", URI.create("https://repo.gradle.org/gradle/libs-releases"), httpClientComponent::getHttpClient);
        lifecycleManager.add(repositoryGradleReleases);

        repositoryVirtual = new DefaultVirtualRepository("public");
        ((DefaultVirtualRepository) repositoryVirtual).add(repositoryMavenCentral);
        ((DefaultVirtualRepository) repositoryVirtual).add(repositoryGradleReleases);
        lifecycleManager.add(repositoryVirtual);

        lifecycleManager.start();
    }

    @Test
    void testRemoteGradleReleases() throws Exception {
        final RequestHandler requestHandler = new DefaultRequestHandler();
        requestHandler.addRepository(repositoryGradleReleases);

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/gradle-releases/org/gradle/gradle-tooling-api/8.2.1/gradle-tooling-api-8.2.1.pom"));
        final boolean exist = requestHandler.exist(resourceRequest);

        assertTrue(exist);
    }

    @Test
    void testRemoteMavenCentral() throws Exception {
        final RequestHandler requestHandler = new DefaultRequestHandler();
        requestHandler.addRepository(repositoryMavenCentral);

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/maven-central/org/apache/maven/plugins/maven-clean-plugin/3.2.0/maven-clean-plugin-3.2.0.pom"));
        final boolean exist = requestHandler.exist(resourceRequest);

        assertTrue(exist);
    }

    @Test
    void testVirtual() throws Exception {
        final RequestHandler requestHandler = new DefaultRequestHandler();
        requestHandler.addRepository(repositoryVirtual);

        // Only in https://repo.gradle.org/gradle/libs-releases
        ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/public/org/gradle/gradle-tooling-api/8.2.1/gradle-tooling-api-8.2.1.pom"));
        boolean exist = requestHandler.exist(resourceRequest);
        assertTrue(exist);

        // Only in https://repo1.maven.org/maven2
        resourceRequest = ResourceRequest.of(URI.create("/public/org/apache/maven/maven/3.8.4/maven-3.8.4.pom"));
        exist = requestHandler.exist(resourceRequest);
        assertTrue(exist);
    }
}
