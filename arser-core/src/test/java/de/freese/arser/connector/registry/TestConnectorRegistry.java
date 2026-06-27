package de.freese.arser.connector.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.freese.arser.EnabledIfReachable;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.api.Result;
import de.freese.arser.connector.core.Attributes;
import de.freese.arser.connector.core.ConnectorRegistry;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.decorator.CachingConnector;
import de.freese.arser.connector.decorator.RetryingConnector;
import de.freese.arser.connector.file.FileConnector;
import de.freese.arser.connector.http.HttpConnector;
import de.freese.arser.connector.security.CredentialsProvider;
import de.freese.arser.connector.security.UriGuard;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
class TestConnectorRegistry {
    // httpbin.org
    // postman-echo.com
    // httpbun.com
    private static final String TEST_HOST = "https://httpbun.com";
    private static final String TEST_URI = TEST_HOST + "/robots.txt";

    private static ConnectorRegistry registry;

    @AfterAll
    static void afterAll() {
        registry.close();
    }

    @BeforeAll
    static void beforeAll() {
        final Connector httpConnector = new HttpConnector(
                UriGuard.denyInternalNetworks(),
                CredentialsProvider.NONE,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10L))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build()
        );
        final Connector httpResilient = new RetryingConnector(
                new CachingConnector(httpConnector, Duration.ofMinutes(5L)),
                3, Duration.ofSeconds(3L));

        final Connector fileConnector = new FileConnector(UriGuard.ALLOW_ALL);

        registry = ConnectorRegistry.autoDiscover()
                .register(httpResilient)
                .register(fileConnector);
    }

    @Test
    @EnabledIfReachable(uri = TEST_HOST, timeoutMillis = 1000)
    void testRegistry() throws Exception {

        // Comfort-Call.
        final boolean exists = registry.call(URI.create(TEST_URI), Operations.EXISTS);
        assertTrue(exists);

        // Download.
        try (BlobValue blobValue = registry.call(URI.create(TEST_URI), Operations.DOWNLOAD)) {
            assertNotNull(blobValue);
            assertTrue(blobValue.getContentLength() > 0L);
        }

        // Upload with typed attributes.
        final ConnectorResponse<Void> responseUpload = registry.execute(ConnectorRequest.of(URI.create(TEST_HOST), Operations.UPLOAD)
                .with(Attributes.BODY, "Hello World".getBytes())
                .with(Attributes.METHOD, "POST"));
        assertNotNull(responseUpload);
        assertEquals(HttpURLConnection.HTTP_OK, responseUpload.meta().get("statusCode"));

        // Safe-Execute with Pattern-Matching.
        final Result<BlobValue> resultDownload = registry.safeExecute(ConnectorRequest.of(URI.create("file:/tmp/missing"), Operations.DOWNLOAD));
        assertNotNull(resultDownload);

        switch (resultDownload) {
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
            }
        }
    }
}
