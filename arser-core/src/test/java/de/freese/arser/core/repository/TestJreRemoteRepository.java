// Created: 21 Dez. 2024
package de.freese.arser.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import de.freese.arser.core.api.JreHttpClientRemoteRepository;
import de.freese.arser.core.api.Repository;
import de.freese.arser.core.api.ResponseHandler;
import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
class TestJreRemoteRepository {
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.pom";

    @Test
    void testExist() throws Exception {
        final String contentRoot = "exist";

        final Repository repository = new JreHttpClientRemoteRepository(contentRoot, URI.create("https://repo1.maven.org/maven2"));
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
    void testNotExist() throws Exception {
        final String contentRoot = "not-exist";

        final Repository repository = new JreHttpClientRemoteRepository(contentRoot, URI.create("https://plugins.gradle.org"));
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
    void testNotWriteable() throws Exception {
        final String contentRoot = "not-writeable";

        final Repository repository = new JreHttpClientRemoteRepository(contentRoot, URI.create("https://something"));
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

    @Test
    void testStreamTo() throws Exception {
        final String contentRoot = "stream-to";

        final Repository repository = new JreHttpClientRemoteRepository(contentRoot, URI.create("https://repo1.maven.org/maven2"));
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/" + RESOURCE));

        final AtomicLong contentLengthAtomicLong = new AtomicLong(0);
        final AtomicReference<byte[]> dataAtomicReference = new AtomicReference<>();

        try {
            repository.streamTo(resourceRequest, new ResponseHandler() {
                @Override
                public void onError(final Exception exception) {
                    fail();
                }

                @Override
                public void onSuccess(final long contentLength, final InputStream inputStream) throws Exception {
                    assertTrue(contentLength > 0);
                    assertNotNull(inputStream);

                    contentLengthAtomicLong.set(contentLength);
                    dataAtomicReference.set(inputStream.readAllBytes());
                }
            });

            assertTrue(contentLengthAtomicLong.get() > 0);
            assertNotNull(dataAtomicReference.get());
            assertEquals(contentLengthAtomicLong.get(), dataAtomicReference.get().length);
        }
        finally {
            repository.stop();
        }
    }
}
