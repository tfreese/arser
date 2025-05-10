// Created: 21 Dez. 2024
package de.freese.arser.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.freese.arser.core.model.RequestResource;
import de.freese.arser.core.model.ResourceRequest;

/**
 * @author Thomas Freese
 */
class TestFileRepository {
    private static final Path PATH_TEST = Path.of(System.getProperty("java.io.tmpdir"), "arser-test-file");
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.pom";

    @AfterAll
    static void afterAll() throws IOException {
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
    static void beforeAll() throws IOException {
        Files.createDirectories(PATH_TEST);
    }

    @Test
    void testExist() throws Exception {
        final String contentRoot = "exist";

        final Repository repository = new FileRepository(contentRoot, PATH_TEST.resolve(contentRoot).toUri(), false);
        repository.start();

        final Path path = Path.of(repository.getBaseUri()).resolve(RESOURCE);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "test");

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/" + RESOURCE));

        try {
            final boolean exist = repository.exist(resourceRequest);

            assertTrue(exist);
        }
        finally {
            repository.stop();
        }
    }

    @Test
    void testExistFail() throws Exception {
        final String contentRoot = "exist-fail";

        final Repository repository = new FileRepository(contentRoot, PATH_TEST.resolve(contentRoot).toUri(), false);
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/" + RESOURCE));

        try {
            final boolean exist = repository.exist(resourceRequest);

            assertFalse(exist);
        }
        finally {
            repository.stop();
        }
    }

    @Test
    void testGetResource() throws Exception {
        final String contentRoot = "download-uri";
        final Path basePath = PATH_TEST.resolve(contentRoot);

        final Repository repository = new FileRepository(contentRoot, basePath.toUri(), false);
        repository.start();

        final Path path = Path.of(repository.getBaseUri()).resolve(RESOURCE);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "test");

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/" + RESOURCE));

        try {
            final RequestResource requestResource = repository.getResource(resourceRequest);

            assertNotNull(requestResource);
        }
        finally {
            repository.stop();
        }
    }

    @Test
    void testWriteable() throws Exception {
        final String contentRoot = "writeable";

        final Repository repository = new FileRepository(contentRoot, PATH_TEST.resolve(contentRoot).toUri(), true);
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/" + RESOURCE));

        try (InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8))) {
            repository.write(resourceRequest, inputStream);
        }
        finally {
            repository.stop();
        }

        final Path path = Path.of(repository.getBaseUri()).resolve(RESOURCE);
        assertTrue(Files.exists(path));

        assertEquals("test", Files.readString(path));
    }

    @Test
    void testWriteableFail() throws Exception {
        final String contentRoot = "writeable-fail";

        final Repository repository = new FileRepository(contentRoot, PATH_TEST.resolve(contentRoot).toUri(), false);
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/" + RESOURCE));

        try (InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8))) {
            final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> repository.write(resourceRequest, inputStream));

            assertNotNull(exception);
            assertTrue(exception.getMessage().startsWith("read only repository: " + contentRoot));
        }
        finally {
            repository.stop();
        }

        final Path path = Path.of(repository.getBaseUri()).resolve(RESOURCE);
        assertFalse(Files.exists(path));
    }
}
