package de.freese.arser.connector.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.freese.arser.EnabledIfReachable;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.api.Result;
import de.freese.arser.connector.core.ConnectorRegistry;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.security.CredentialsProvider;
import de.freese.arser.connector.security.UriGuard;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
class TestHttpConnector {
    // httpbin.org
    // httpbin.io
    // httpbun.com
    // postman-echo.com
    // "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom"
    private static final String TEST_URI = "https://httpbin.io/robots.txt";

    private static Connector connector;

    @AfterAll
    static void afterAll() throws Exception {
        connector.stop();
    }

    @BeforeAll
    static void beforeAll() {
        connector = new JreHttpClientConnector(UriGuard.ALLOW_ALL, CredentialsProvider.NONE, HttpClient.newBuilder().build());
    }

    @Test
    @EnabledIfReachable(uri = TEST_URI, timeoutMillis = 1000)
    void testHttpConnector() throws Exception {
        final ConnectorRequest<BlobValue> connectorRequest = ConnectorRequest.of(URI.create(TEST_URI), Operations.DOWNLOAD);

        final ConnectorResponse<BlobValue> connectorResponse = connector.execute(connectorRequest);
        assertNotNull(connectorResponse);
        assertEquals(HttpURLConnection.HTTP_OK, connectorResponse.meta().get("statusCode"));

        try (BlobValue bv = connectorResponse.value()) {
            assertNotNull(bv);
            assertTrue(bv.getContentLength() > 0L);
        }

        final CompletableFuture<ConnectorResponse<BlobValue>> completableFuture = connector.executeAsync(connectorRequest);
        assertNotNull(completableFuture);
        assertNotNull(completableFuture.get());
        assertEquals(HttpURLConnection.HTTP_OK, completableFuture.get().meta().get("statusCode"));

        try (BlobValue bv = completableFuture.get().value()) {
            assertNotNull(bv);
            assertTrue(bv.getContentLength() > 0L);
        }

        final Result<BlobValue> result = connector.safeExecute(connectorRequest);
        assertNotNull(result);
        assertTrue(result.isSuccess());

        switch (result) {
            case final Result.Success<BlobValue> s -> {
                assertNotNull(s.response());
                assertTrue(s.isSuccess());

                try (BlobValue bv = s.response().value()) {
                    assertNotNull(bv);
                    assertTrue(bv.getContentLength() > 0L);
                }
            }
            case final Result.NotFound<BlobValue> nf -> {
                assertNotNull(nf.uri());
                assertFalse(nf.isSuccess());
            }
            case final Result.Failure<BlobValue> f -> {
                assertNotNull(f.cause());
                assertFalse(f.isSuccess());
                fail();
            }
        }

        final ConnectorRegistry connectorRegistry = ConnectorRegistry.autoDiscover().register(connector);
        final BlobValue blobValue = connectorRegistry.call(connectorRequest.uri(), connectorRequest.operation());
        assertNotNull(blobValue);
        assertTrue(blobValue.getContentLength() > 0L);
    }
}
