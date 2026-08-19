package de.freese.arser.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import de.freese.arser.api.Arser;
import de.freese.arser.api.ArserConfig;
import de.freese.arser.component.DefaultLifeCycleRegistry;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.config.ThreadPoolConfig;
import de.freese.arser.repository.file.FileRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 * @since 22.10.2024
 */
class TestJreServer {
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom";
    private static HttpClient httpClient;
    private static LifeCycleRegistry lifeCycleRegistry;
    private static int localPort;

    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static Path pathTest;

    @AfterAll
    static void afterAll() throws Exception {
        lifeCycleRegistry.stop();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        final ArserConfig arserConfig = ArserConfig.builder()
                .addHttpRepositoryConfig(HttpRepositoryConfig.builder()
                        .uri(URI.create("https://repo1.maven.org/maven2"))
                        .name("maven-central")
                        .withRetrying(3, Duration.ofSeconds(2L))
                        .cachingPath(pathTest.resolve("maven-central-cache"))
                        .withLogging()
                        .build())
                .addFileRepositoryConfig(FileRepositoryConfig.builder()
                        .uri(pathTest.resolve("snapshots").toUri())
                        .name("snapshots")
                        .readOnly(false)
                        .withLogging()
                        .build())
                .serverConfig(ServerConfig.builder()
                        .port(ArserUtils.findRandomOpenPort())
                        .threadPoolConfig(ThreadPoolConfig.builderServerDefault()
                                .build())
                        .build())
                .build();

        localPort = arserConfig.serverConfig().port();

        lifeCycleRegistry = new DefaultLifeCycleRegistry();

        final Arser arser = Arser.from(arserConfig, lifeCycleRegistry);

        final JreHttpServer jreHttpServer = new JreHttpServer(arser);
        lifeCycleRegistry.register(jreHttpServer);

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(30));

        httpClient = httpClientBuilder.build();
        lifeCycleRegistry.register(httpClient);

        lifeCycleRegistry.start();
    }

    private static URI getServerUri() {
        return URI.create("http://localhost:" + localPort);
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
            assertEquals("HTTP-STATUS: 404 for https://repo1.maven.org/maven2/a" + RESOURCE, content);
        }
    }

    @Test
    void testWriteable() throws Exception {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(getServerUri().resolve("snapshots/" + RESOURCE))
                .PUT(HttpRequest.BodyPublishers.ofString("test"))
                .build();

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(ArserUtils.HTTP_STATUS_OK, httpResponse.statusCode());

        final Path path = pathTest.resolve("snapshots").resolve(RESOURCE);
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
