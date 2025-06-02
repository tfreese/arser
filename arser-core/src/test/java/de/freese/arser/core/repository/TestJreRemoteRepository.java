// Created: 21 Dez. 2024
package de.freese.arser.core.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import de.freese.arser.EnabledIfReachable;
import de.freese.arser.core.model.FileResource;
import de.freese.arser.core.model.ResourceRequest;

/**
 * @author Thomas Freese
 */
class TestJreRemoteRepository {
    // private static final Path PATH_TEST = Path.of(System.getProperty("java.io.tmpdir"), "arser-test-remote");
    private static final String REMOTE_REPO = "https://repo1.maven.org/maven2";
    private static final URI REMOTE_REPO_URI = URI.create(REMOTE_REPO);
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.pom";

    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static Path pathTest;

    private static Path getPathTest() {
        return pathTest;
    }

    // @AfterAll
    // static void afterAll() throws IOException {
    //     if (!Files.exists(PATH_TEST)) {
    //         return;
    //     }
    //
    //     Files.walkFileTree(PATH_TEST, new SimpleFileVisitor<>() {
    //         @Override
    //         public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
    //             Files.delete(dir);
    //             return FileVisitResult.CONTINUE;
    //         }
    //
    //         @Override
    //         public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
    //             Files.delete(file);
    //             return FileVisitResult.CONTINUE;
    //         }
    //     });
    //
    //     Files.deleteIfExists(PATH_TEST);
    // }

    @Test
    @EnabledIfReachable(uri = REMOTE_REPO, timeoutMillis = 1000)
    void testExist() throws Exception {
        final String contentRoot = "exist";

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, REMOTE_REPO_URI, getPathTest());
        repository.start();

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
    @EnabledIfReachable(uri = REMOTE_REPO, timeoutMillis = 1000)
    void testExistFail() throws Exception {
        final String contentRoot = "exist-fail";

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, REMOTE_REPO_URI, getPathTest());
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/a" + RESOURCE));

        try {
            final boolean exist = repository.exist(resourceRequest);

            assertFalse(exist);
        }
        finally {
            repository.stop();
        }
    }

    @Test
    @EnabledIfReachable(uri = REMOTE_REPO, timeoutMillis = 1000)
    void testGetDownloadUri() throws Exception {
        final String contentRoot = "download-uri";

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, REMOTE_REPO_URI, getPathTest());
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/" + RESOURCE));

        try {
            final FileResource fileResource = repository.getResource(resourceRequest);

            assertNotNull(fileResource);
        }
        finally {
            repository.stop();
        }
    }

    @Test
    @EnabledIfReachable(uri = REMOTE_REPO, timeoutMillis = 1000)
    void testGetDownloadUriFail() throws Exception {
        final String contentRoot = "download-uri-fail";

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, REMOTE_REPO_URI, getPathTest());
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/a" + RESOURCE));

        try {
            final FileResource fileResource = repository.getResource(resourceRequest);

            assertNull(fileResource);
        }
        finally {
            repository.stop();
        }
    }

    @Test
    @EnabledIfReachable(uri = REMOTE_REPO, timeoutMillis = 1000)
    void testWriteableFail() throws Exception {
        final String contentRoot = "writeable-fail";

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, URI.create("https://something"), getPathTest());
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
    }
}
