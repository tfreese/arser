// Created: 21 Dez. 2024
package de.freese.arser.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResponseHandler;

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
    void testExistNot() throws Exception {
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
    void testStreamTo() throws Exception {
        final String contentRoot = "stream-to";
        final URI uri = URI.create("https://repo1.maven.org/maven2");

        final Repository repository = new JreHttpClientRemoteRepository(contentRoot, uri);
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
                public void onSuccess(final long contentLength, final InputStream inputStream) {
                    assertTrue(contentLength > 0);
                    assertNotNull(inputStream);

                    contentLengthAtomicLong.set(contentLength);

                    try {
                        dataAtomicReference.set(inputStream.readAllBytes());
                    }
                    catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
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

    @Test
    void testStreamToFail() throws Exception {
        final String contentRoot = "stream-to-fail";
        final URI uri = URI.create("https://plugins.gradle.org");

        final Repository repository = new JreHttpClientRemoteRepository(contentRoot, uri);
        repository.start();

        final ResourceRequest resourceRequest = ResourceRequest.of(URI.create("/" + contentRoot + "/" + RESOURCE));

        final AtomicReference<Exception> atomicReference = new AtomicReference<>();

        try {
            repository.streamTo(resourceRequest, new ResponseHandler() {
                @Override
                public void onError(final Exception exception) {
                    atomicReference.set(exception);
                }

                @Override
                public void onSuccess(final long contentLength, final InputStream inputStream) {
                    fail();
                }
            });

            assertNotNull(atomicReference.get());
            assertEquals(IOException.class, atomicReference.get().getClass());
            assertEquals("HTTP-STATUS: 404 for " + uri.resolve(RESOURCE), atomicReference.get().getMessage());
        }
        finally {
            repository.stop();
        }
    }

    @Test
    void testWriteableNot() throws Exception {
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
}
