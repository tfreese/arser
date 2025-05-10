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

import org.junit.jupiter.api.Test;

import de.freese.arser.EnabledIfReachable;
import de.freese.arser.core.model.RequestResource;
import de.freese.arser.core.model.ResourceRequest;

/**
 * @author Thomas Freese
 */
class TestJreRemoteRepository {
    private static final String REMOTE_REPO = "https://repo1.maven.org/maven2";
    private static final URI REMOTE_REPO_URI = URI.create(REMOTE_REPO);
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.pom";

    @Test
    @EnabledIfReachable(uri = REMOTE_REPO, timeoutMillis = 1000)
    void testExist() throws Exception {
        final String contentRoot = "exist";

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, REMOTE_REPO_URI);
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

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, REMOTE_REPO_URI);
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

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, REMOTE_REPO_URI);
        repository.start();

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
    @EnabledIfReachable(uri = REMOTE_REPO, timeoutMillis = 1000)
    void testGetDownloadUriFail() throws Exception {
        final String contentRoot = "download-uri-fail";

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, REMOTE_REPO_URI);
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/a" + RESOURCE));

        try {
            final RequestResource requestResource = repository.getResource(resourceRequest);

            assertNull(requestResource);
        }
        finally {
            repository.stop();
        }
    }

    @Test
    @EnabledIfReachable(uri = REMOTE_REPO, timeoutMillis = 1000)
    void testWriteableFail() throws Exception {
        final String contentRoot = "writeable-fail";

        final Repository repository = new RemoteRepositoryJreHttpClient(contentRoot, URI.create("https://something"));
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
