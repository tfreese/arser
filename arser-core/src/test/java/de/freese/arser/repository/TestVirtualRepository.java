package de.freese.arser.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.component.DefaultLifeCycleRegistry;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.connector.decorator.LoggingConnector;
import de.freese.arser.connector.file.FileConnector;
import de.freese.arser.connector.http.JreHttpClientConnector;
import de.freese.arser.connector.security.CredentialsProvider;
import de.freese.arser.connector.security.UriGuard;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.model.ArserRequest;
import de.freese.arser.model.ArserResult;
import de.freese.arser.repository.file.FileRepository;
import de.freese.arser.repository.http.HttpRepository;
import de.freese.arser.repository.virtual.VirtualRepository;

/**
 * @author Thomas Freese
 */
class TestVirtualRepository {
    private static LifeCycleRegistry lifeCycleRegistry;
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static Path pathTest;
    private static VirtualRepository virtualRepository;

    @AfterAll
    static void afterAll() throws Exception {
        lifeCycleRegistry.stop();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        lifeCycleRegistry = new DefaultLifeCycleRegistry();

        virtualRepository = new VirtualRepository("test");

        final Repository repositoryFile = new FileRepository(pathTest.toUri(), "maven-local", new FileConnector(), false);
        virtualRepository.add(repositoryFile);

        final Connector connectorHttp = new JreHttpClientConnector(UriGuard.ALLOW_ALL, CredentialsProvider.NONE, HttpClient.newBuilder().build());
        final Connector connectorHttpLogging = new LoggingConnector(connectorHttp);
        lifeCycleRegistry.register(connectorHttpLogging);

        final Repository repositoryHttp = new HttpRepository(URI.create("https://repo1.maven.org/maven2"), "central", connectorHttpLogging);
        lifeCycleRegistry.register(repositoryHttp);

        virtualRepository.add(repositoryHttp);

        lifeCycleRegistry.start();
    }

    @Test
    void testDownload() throws Exception {
        final ArserResult<?> arserResult = virtualRepository.download(ArserRequest.of("org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom"));
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.Download<?>(final BlobValue blobValue)) {
            assertNotNull(blobValue);
            assertTrue(blobValue.getContentLength() > 0L);
        } else {
            fail();
        }
    }

    @Test
    void testExist() {
        final ArserResult<?> arserResult = virtualRepository.exist(ArserRequest.of("org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom"));
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.Exist<?>(final boolean exist)) {
            assertTrue(exist);
        } else {
            fail();
        }
    }

    @Test
    void testUpload() {
        final ArserResult<?> arserResult = virtualRepository.upload(ArserRequest.of("org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom"), InputStream.nullInputStream());
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.Upload<?>) {
            fail();
        } else {
            assertTrue(true);
        }
    }
}
