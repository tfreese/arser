package de.freese.arser.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.component.DefaultLifeCycleRegistry;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.repository.file.FileRepository;
import de.freese.arser.repository.file.FileRepositoryConfig;
import de.freese.arser.repository.http.HttpRepository;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.virtual.VirtualRepository;
import de.freese.arser.repository.virtual.VirtualRepositoryConfig;

/**
 * @author Thomas Freese
 */
class TestVirtualRepository {
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom";

    private static LifeCycleRegistry lifeCycleRegistry;

    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static Path pathTest;

    private static Repository virtualRepository;

    @AfterAll
    static void afterAll() throws Exception {
        lifeCycleRegistry.stop();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        lifeCycleRegistry = new DefaultLifeCycleRegistry();

        final FileRepositoryConfig fileRepositoryConfig = FileRepositoryConfig.builder()
                .name("maven-local")
                .uri(pathTest.toUri())
                .withLogging()
                .readOnly(false)
                .build();
        final Repository fileRepository = FileRepository.of(fileRepositoryConfig, lifeCycleRegistry);

        // final Connector connectorHttp = new JreHttpClientConnector(UriGuard.ALLOW_ALL, CredentialsProvider.NONE, HttpClient.newBuilder().build());
        // // final Connector connectorHttpLogging = new LoggingConnector(connectorHttp);
        // lifeCycleRegistry.register(connectorHttpLogging);

        final HttpRepositoryConfig httpRepositoryConfig = HttpRepositoryConfig.builder()
                .name("central")
                .uri(URI.create("https://repo1.maven.org/maven2"))
                .withRetrying(3, Duration.ofSeconds(2L))
                .withLogging()
                .build();
        final Repository httpRepository = HttpRepository.of(httpRepositoryConfig, lifeCycleRegistry);

        final VirtualRepositoryConfig virtualRepositoryConfig = VirtualRepositoryConfig.builder()
                .name("test")
                .uri(URI.create("virtual://test"))
                .addRepositoryRef("maven-local")
                .addRepositoryRef("central")
                .build();

        virtualRepository = VirtualRepository.of(virtualRepositoryConfig, repoName -> switch (repoName) {
            case "maven-local" -> fileRepository;
            case "central" -> httpRepository;
            default -> throw new IllegalStateException("Repository not found: " + repoName);
        });

        lifeCycleRegistry.start();
    }

    @Test
    void testDownload() throws Exception {
        final ArserResult arserResult = virtualRepository.download(ArserRequest.of(RESOURCE));
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.Download(final BlobValue blobValue)) {
            assertNotNull(blobValue);
            assertTrue(blobValue.getContentLength() > 0L);
        }
        else {
            fail();
        }
    }

    @Test
    void testExist() {
        final ArserResult arserResult = virtualRepository.exist(ArserRequest.of(RESOURCE));
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.Exist(final URI uri)) {
            assertNotNull(uri);
        }
        else {
            fail();
        }
    }

    @Test
    void testUpload() {
        final ArserResult arserResult = virtualRepository.upload(ArserRequest.of(RESOURCE), InputStream.nullInputStream());
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.Upload) {
            fail();
        }
        else {
            assertTrue(true);
        }
    }
}
