package de.freese.arser.core.connector.http;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.junit.jupiter.api.Test;

import de.freese.arser.core.connector.Connector;

/**
 * @author Thomas Freese
 */
class TestHttpClientConnector {
    private static final URI HTTP_REPO_URI = URI.create("https://repo1.maven.org/maven2");

    @Test
    void testJreHttpClientConnectorBuilder() throws Exception {
        final Connector connector = new HttpClientConnectorBuilder()
                .uri(HTTP_REPO_URI)
                .build();

        assertNotNull(connector);

        connector.stop();
    }
}
