package de.freese.arser.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.component.DefaultLifeCycleRegistry;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.model.ArserRequest;
import de.freese.arser.model.ArserResult;
import de.freese.arser.repository.file.FileRepository;

/**
 * @author Thomas Freese
 */
class TestFileRepository {

    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static Path pathTest;

    private LifeCycleRegistry lifeCycleRegistry;

    @AfterEach
    void afterEach() throws Exception {
        lifeCycleRegistry.stop();
    }

    @BeforeEach
    void beforeEach() {
        lifeCycleRegistry = new DefaultLifeCycleRegistry();
    }

    @Test
    void testFileRepository() throws Exception {
        final Repository repository = FileRepository.builder()
                .uri(pathTest.toUri())
                .name("maven-local")
                .readOnly(false)
                .withLogging()
                .build(lifeCycleRegistry);

        lifeCycleRegistry.start();

        // Exist.
        ArserResult<?> arserResult = repository.exist(ArserRequest.of("a/b/c/1/c-1.txt"));
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.NotFound<?>(final URI uri)) {
            assertNotNull(uri);
            assertEquals(pathTest.resolve("a/b/c/1/c-1.txt").toUri(), uri);
        } else {
            fail();
        }

        // Upload.
        try (InputStream inputStream = new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8))) {
            arserResult = repository.upload(ArserRequest.of("a/b/c/1/c-1.txt"), inputStream);
            assertNotNull(arserResult);

            if (arserResult instanceof ArserResult.Upload<?>(final long contentLength)) {
                assertTrue(contentLength > 0L);
            } else {
                fail();
            }
        }

        // Exist again.
        arserResult = repository.exist(ArserRequest.of("a/b/c/1/c-1.txt"));
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.Exist<?>(final boolean exist)) {
            assertTrue(exist);
        } else {
            fail();
        }

        // Download.
        arserResult = repository.download(ArserRequest.of("a/b/c/1/c-1.txt"));
        assertNotNull(arserResult);

        if (arserResult instanceof ArserResult.Download<?>(final BlobValue blobValue)) {
            assertNotNull(blobValue);
            assertTrue(blobValue.getContentLength() > 0L);

            try (InputStream inputStream = blobValue.createInputStream()) {
                assertEquals("Hello World", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } else {
            fail();
        }
    }

    @Test
    void testFileRepositoryReadOnly() throws Exception {
        final Repository repository = FileRepository.builder()
                .uri(pathTest.toUri())
                .name("maven-local")
                .readOnly(true)
                .withLogging()
                .build(lifeCycleRegistry);

        lifeCycleRegistry.start();

        try (InputStream inputStream = new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8))) {
            final ArserResult<?> arserResult = repository.upload(ArserRequest.of("a/b/c/1/c-1.txt"), inputStream);
            assertNotNull(arserResult);

            if (arserResult instanceof ArserResult.Failure<?>(final Throwable cause)) {
                assertNotNull(cause);
                assertEquals(RepositoryException.class, cause.getClass());
                assertEquals("repository is read only: maven-local [FileRepository]", cause.getMessage());
            } else {
                fail();
            }
        }
    }
}
