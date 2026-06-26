package de.freese.arser.connector;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

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
public final class ConnectorDemo {
    static void main() {
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

        // httpbin.org
        // postman-echo.com
        // httpbun.com
        // final String webServer = "https://httpbin.org";
        final String webServer = "https://httpbun.com";

        try (ConnectorRegistry registry = ConnectorRegistry.autoDiscover()
                .register(httpResilient)
                .register(fileConnector)) {

            // Comfort-Call.
            final boolean exists = registry.call(URI.create(webServer + "/robots.txt"), Operations.EXISTS);

            // Download as byte[].
            final byte[] payload = registry.call(URI.create(webServer + "/robots.txt"), Operations.DOWNLOAD);
            // final ConnectorResponse<byte[]> download = registry.execute(ConnectorRequest.of(URI.create(webServer + "/robots.txt"), Operations.DOWNLOAD));
            // final byte[] payload = download.value();

            // Upload with typed attributes.
            final ConnectorResponse<Void> upload = registry.execute(ConnectorRequest.of(URI.create(webServer), Operations.UPLOAD)
                    .with(Attributes.BODY, "hello".getBytes())
                    .with(Attributes.METHOD, "POST"));

            // Safe-Execute with Pattern-Matching.
            final Result<byte[]> result = registry.safeExecute(ConnectorRequest.of(URI.create("file:/tmp/missing"), Operations.DOWNLOAD));

            switch (result) {
                case final Result.Success<byte[]> s -> System.out.println(s.response().value().length);
                case final Result.NotFound<byte[]> nf -> System.out.println("not found: " + nf.uri());
                case final Result.Failure<byte[]> f -> f.cause().printStackTrace();
            }
        }
    }

    private ConnectorDemo() {
        super();
    }
}
