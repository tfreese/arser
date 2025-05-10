// Created: 23 Dez. 2024
package de.freese.arser.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * @author Thomas Freese
 */
@AutoConfigureWebTestClient(timeout = "10000")
abstract class AbstractTestSpringServer {
    private static final String RESOURCE = "org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.pom";

    static void afterAll(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
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

        Files.deleteIfExists(path);
    }

    // @LocalServerPort
    // private int localServerPort;

    @Resource
    private WebTestClient webTestClient;

    @Test
    void testExist() {
        webTestClient.head()
                .uri("public/" + RESOURCE)
                .exchange()
                .expectStatus()
                .isOk()
        ;
    }

    @Test
    void testExistFail() {
        webTestClient.head()
                .uri("public/a" + RESOURCE)
                .exchange()
                .expectStatus()
                .isNotFound()
        ;
    }

    @Test
    void testGet() {
        webTestClient.get()
                .uri("public/" + RESOURCE)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(InputStreamResource.class)
                .value(inputStreamResource -> {
                    assertNotNull(inputStreamResource);

                    try (InputStream inputStream = inputStreamResource.getInputStream()) {
                        assertNotNull(inputStream);
                        assertTrue(inputStreamResource.contentLength() > 2000L);
                    }
                    catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                })
        ;
    }

    @Test
    void testGetFail() {
        webTestClient.get()
                .uri("public/a" + RESOURCE)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(InputStreamResource.class)
                .value(inputStreamResource -> {
                    assertNotNull(inputStreamResource);

                    try {
                        final String message = inputStreamResource.getContentAsString(StandardCharsets.UTF_8);
                        assertNotNull(message);
                        assertTrue(message.startsWith("HTTP-STATUS: 404"));
                    }
                    catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                })
        ;
    }

    @Test
    void testWriteable() throws IOException {
        webTestClient.put()
                .uri("deploy-snapshots/" + RESOURCE)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromResource(new InputStreamResource(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)))))
                .exchange()
                .expectStatus()
                .isOk()
        ;

        // See SpringConfig.
        final Path path = getWorkingDir().resolve("deploy-snapshots").resolve(RESOURCE);
        assertTrue(Files.exists(path));

        assertEquals("test", Files.readString(path));
    }

    @Test
    void testWriteableFail() {
        webTestClient.put()
                .uri("public/" + RESOURCE)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromResource(new InputStreamResource(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)))))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .value(value -> {
                    assertNotNull(value);
                    assertEquals("read only repository: public - virtual", value);
                })
        ;
    }

    protected abstract Path getWorkingDir();
}
