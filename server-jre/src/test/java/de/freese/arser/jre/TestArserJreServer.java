// Created: 22 Dez. 2024
package de.freese.arser.jre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.freese.arser.config.ArserConfig;
import de.freese.arser.config.FileRepositoryConfig;
import de.freese.arser.config.RemoteRepositoryConfig;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.instance.ArserInstance;
import de.freese.arser.instance.ArserInstanceFactory;
import de.freese.arser.jre.server.JreHttpServer;

/**
 * @author Thomas Freese
 */
class TestArserJreServer {
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.pom";

    private static ArserInstance arserInstance;
    private static HttpClient httpClient;
    private static JreHttpServer proxyServer;

    @AfterAll
    static void afterAll() throws Exception {
        httpClient.close();
        proxyServer.stop();

        ArserInstanceFactory.shutdownAll();

        if (!Files.exists(arserInstance.getConfig().getWorkingDir())) {
            return;
        }

        Files.walkFileTree(arserInstance.getConfig().getWorkingDir(), new SimpleFileVisitor<>() {
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

        Files.deleteIfExists(arserInstance.getConfig().getWorkingDir());
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        final int randomPort = ArserUtils.findRandomOpenPort();

        final Path workingPath = Path.of(System.getProperty("java.io.tmpdir"), "arser-test-server-jre");

        final ArserConfig config = ArserConfig.builder()
                .workingDir(workingPath)
                .addRemoteRepository(RemoteRepositoryConfig.builder()
                        .contextRoot("maven-central")
                        .uri(URI.create("https://repo1.maven.org/maven2"))
                        .build())
                .addFileRepository(FileRepositoryConfig.builder()
                        .contextRoot("deploy-snapshots")
                        .uri(workingPath.resolve("deploy-snapshots").toUri())
                        .writeable(true)
                        .build())
                .serverConfig(ServerConfig.builder()
                        .port(randomPort)
                        .build())
                .build();

        arserInstance = ArserInstanceFactory.createArserInstance(config);

        proxyServer = new JreHttpServer(arserInstance);
        proxyServer.start();

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(30));

        httpClient = httpClientBuilder.build();
    }

    private static URI getServerUri() {
        return URI.create("http://localhost:" + arserInstance.getConfig().getServerConfig().getPort());
    }

    @Test
    void testExist() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(getServerUri().resolve("maven-central/" + RESOURCE))
                .HEAD()
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_OK, httpResponse.statusCode());
    }

    @Test
    void testExistFail() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(getServerUri().resolve("maven-central/a" + RESOURCE))
                .HEAD()
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_NOT_FOUND, httpResponse.statusCode());
    }

    @Test
    void testGet() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(getServerUri().resolve("maven-central/" + RESOURCE))
                .GET()
                .build();

        final HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(ArserUtils.HTTP_STATUS_OK, httpResponse.statusCode());

        try (InputStream inputStream = httpResponse.body()) {
            assertNotNull(inputStream);

            final String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertNotNull(content);
            assertTrue(content.length() > 2000);
        }
    }

    @Test
    void testGetFail() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(getServerUri().resolve("maven-central/a" + RESOURCE))
                .GET()
                .build();

        final HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(ArserUtils.HTTP_STATUS_NOT_FOUND, httpResponse.statusCode());

        try (InputStream inputStream = httpResponse.body()) {
            assertNotNull(inputStream);

            final String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertNotNull(content);
            assertEquals("HTTP-STATUS: 404 for /a" + RESOURCE, content);
        }
    }

    @Test
    void testWriteable() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(getServerUri().resolve("deploy-snapshots/" + RESOURCE))
                .PUT(HttpRequest.BodyPublishers.ofString("test"))
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_OK, httpResponse.statusCode());

        final Path path = arserInstance.getConfig().getWorkingDir().resolve("deploy-snapshots").resolve(RESOURCE);
        assertTrue(Files.exists(path));
        assertEquals("test", Files.readString(path));
    }

    @Test
    void testWriteableFail() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(getServerUri().resolve("maven-central/" + RESOURCE))
                .PUT(HttpRequest.BodyPublishers.ofString("test"))
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_FORBIDDEN, httpResponse.statusCode());
    }
}
