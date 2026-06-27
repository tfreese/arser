package de.freese.arser.connector.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.api.Result;
import de.freese.arser.connector.core.Attributes;
import de.freese.arser.connector.core.ConnectorRegistry;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.security.UriGuard;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
class TestFileConnector {
    private static Connector connector;

    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static Path pathTest;

    @AfterAll
    static void afterAll() {
        connector.close();
    }

    @BeforeAll
    static void beforeAll() {
        connector = new FileConnector(UriGuard.ALLOW_ALL);
    }

    @Test
    void testFileConnector() throws Exception {
        final Path path = pathTest.resolve("test.txt");

        // Exist
        final ConnectorRequest<Boolean> requestExist = ConnectorRequest.of(path.toUri(), Operations.EXISTS);

        final ConnectorResponse<Boolean> responseExist = connector.execute(requestExist);
        assertNotNull(responseExist);
        assertFalse(responseExist.value());

        final CompletableFuture<ConnectorResponse<Boolean>> completableFutureExist = connector.executeAsync(requestExist);
        assertNotNull(completableFutureExist);
        assertNotNull(completableFutureExist.get());
        assertNotNull(completableFutureExist.get().value());
        assertFalse(completableFutureExist.get().value());

        final Result<Boolean> resultExist = connector.safeExecute(requestExist);
        assertNotNull(resultExist);
        assertTrue(resultExist.isSuccess());

        switch (resultExist) {
            case final Result.Success<Boolean> s -> {
                assertNotNull(s.response());
                assertNotNull(s.response().value());
                assertFalse(s.response().value());
            }
            case final Result.NotFound<Boolean> nf -> {
                assertNotNull(nf.uri());
                assertFalse(nf.isSuccess());
            }
            case final Result.Failure<Boolean> f -> {
                assertNotNull(f.cause());
                assertFalse(f.isSuccess());
                fail();
            }
        }

        final ConnectorRegistry connectorRegistry = ConnectorRegistry.autoDiscover().register(connector);
        final Boolean valueExist = connectorRegistry.call(requestExist.uri(), requestExist.operation());
        assertNotNull(valueExist);
        assertFalse(valueExist);

        // Upload without Body.
        final Exception exception = assertThrows(NoSuchElementException.class, () -> connector.execute(ConnectorRequest.of(path.toUri(), Operations.UPLOAD)));
        assertNotNull(exception);

        // Upload
        final ConnectorRequest<Void> requestUpload = ConnectorRequest.of(path.toUri(), Operations.UPLOAD)
                .with(Attributes.BODY, "Hello World".getBytes(StandardCharsets.UTF_8));

        final ConnectorResponse<Void> responseUpload = connector.execute(requestUpload);
        assertNotNull(responseUpload);
        assertNotNull(responseUpload.meta().get("size"));

        final ConnectorResponse<Boolean> responseUploadExist = connector.execute(requestExist);
        assertNotNull(responseUploadExist);
        assertTrue(responseUploadExist.value());

        final ConnectorResponse<BlobValue> responseDownload = connector.execute(ConnectorRequest.of(path.toUri(), Operations.DOWNLOAD));
        assertNotNull(responseDownload);
        assertNotNull(responseDownload.value());

        try (BlobValue bv = responseDownload.value()) {
            assertNotNull(bv);
            assertTrue(bv.getContentLength() > 0L);

            try (InputStream inputStream = bv.createInputStream()) {
                assertEquals("Hello World", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }
}
