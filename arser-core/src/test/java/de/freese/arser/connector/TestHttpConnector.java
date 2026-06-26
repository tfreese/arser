package de.freese.arser.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.api.Result;
import de.freese.arser.connector.core.ConnectorRegistry;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.http.HttpConnector;
import de.freese.arser.connector.security.CredentialsProvider;
import de.freese.arser.connector.security.UriGuard;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
class TestHttpConnector {
    // httpbin.org
    // postman-echo.com
    // httpbun.com
    // private static final URI TEST_URI = java.net.URI.create("https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom");
    private static final URI TEST_URI = java.net.URI.create("https://httpbun.com/robots.txt");

    @Test
    void testJreHttpClientConnectorBuilder() throws Exception {
        final Connector connector = new HttpConnector(UriGuard.ALLOW_ALL, CredentialsProvider.NONE, HttpClient.newBuilder().build());

        final ConnectorRequest<byte[]> connectorRequest = ConnectorRequest.of(URI.create(TEST_URI.toString()), Operations.DOWNLOAD);

        final ConnectorResponse<byte[]> connectorResponse = connector.execute(connectorRequest);
        assertNotNull(connectorResponse);
        assertNotNull(connectorResponse.value());
        assertEquals(HttpURLConnection.HTTP_OK, connectorResponse.meta().get("statusCode"));

        final CompletableFuture<ConnectorResponse<byte[]>> completableFuture = connector.executeAsync(connectorRequest);
        assertNotNull(completableFuture);
        assertNotNull(completableFuture.get());
        assertNotNull(completableFuture.get().value());

        final Result<byte[]> result = connector.safeExecute(connectorRequest);
        assertNotNull(result);
        assertTrue(result.isSuccess());

        switch (result) {
            case final Result.Success<byte[]> s -> {
                assertNotNull(s.response());
                assertNotNull(s.response().value());
            }
            case final Result.NotFound<byte[]> nf -> fail();
            case final Result.Failure<byte[]> f -> fail();
        }

        final ConnectorRegistry connectorRegistry = ConnectorRegistry.autoDiscover().register(connector);
        final byte[] data = connectorRegistry.call(connectorRequest.uri(), connectorRequest.operation());
        assertNotNull(data);

        connector.close();
    }
}
