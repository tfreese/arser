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

import de.freese.arser.Arser;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.RemoteRepositoryJreHttpClient;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.jre.server.JreHttpServer;

/**
 * @author Thomas Freese
 */
class TestArserJreServer {
    private static final Path PATH_TEST = Path.of(System.getProperty("java.io.tmpdir"), "arser-test-server-jre");
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.pom";
    private static Arser arser;
    private static HttpClient httpClient;
    private static URI localhostServer;
    private static JreHttpServer proxyServer;

    @AfterAll
    static void afterAll() throws Exception {
        httpClient.close();
        proxyServer.stop();

        arser.forEach(repo -> {
            try {
                repo.stop();
            }
            catch (RuntimeException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        if (!Files.exists(PATH_TEST)) {
            return;
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

        Files.deleteIfExists(PATH_TEST);
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        arser = new Arser();
        arser.addRepository(new RemoteRepositoryJreHttpClient("maven-central", URI.create("https://repo1.maven.org/maven2")));
        arser.addRepository(new FileRepository("deploy-snapshots", PATH_TEST.resolve("deploy-snapshots").toUri(), true));
        arser.forEach(repo -> {
            try {
                repo.start();
            }
            catch (RuntimeException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        final int randomPort = ArserUtils.findRandomOpenPort();

        localhostServer = URI.create("http://localhost:" + randomPort);

        proxyServer = new JreHttpServer(arser, ServerConfig.builder()
                .threadNamePattern("http-server-%d")
                .threadPoolCoreSize(2)
                .threadPoolMaxSize(6)
                .port(randomPort)
                .build()
        );
        proxyServer.start();

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(30));

        httpClient = httpClientBuilder.build();
    }

    @Test
    void testExist() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(localhostServer.resolve("maven-central/" + RESOURCE))
                .HEAD()
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_OK, httpResponse.statusCode());
    }

    @Test
    void testExistFail() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(localhostServer.resolve("maven-central/a" + RESOURCE))
                .HEAD()
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_NOT_FOUND, httpResponse.statusCode());
    }

    @Test
    void testGet() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(localhostServer.resolve("maven-central/" + RESOURCE))
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
                .uri(localhostServer.resolve("maven-central/a" + RESOURCE))
                .GET()
                .build();

        final HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(ArserUtils.HTTP_STATUS_NOT_FOUND, httpResponse.statusCode());

        try (InputStream inputStream = httpResponse.body()) {
            assertNotNull(inputStream);

            final String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertNotNull(content);
            assertEquals("HTTP-STATUS: 404 for https://repo1.maven.org/maven2/a" + RESOURCE, content);
        }
    }

    @Test
    void testWriteable() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(localhostServer.resolve("deploy-snapshots/" + RESOURCE))
                .PUT(HttpRequest.BodyPublishers.ofString("test"))
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_OK, httpResponse.statusCode());

        final Path path = PATH_TEST.resolve("deploy-snapshots").resolve(RESOURCE);
        assertTrue(Files.exists(path));
        assertEquals("test", Files.readString(path));
    }

    @Test
    void testWriteableFail() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(localhostServer.resolve("maven-central/" + RESOURCE))
                .PUT(HttpRequest.BodyPublishers.ofString("test"))
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_FORBIDDEN, httpResponse.statusCode());
    }
}
