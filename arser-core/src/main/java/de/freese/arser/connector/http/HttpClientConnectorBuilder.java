package de.freese.arser.connector.http;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

import de.freese.arser.connector.AbstractConnectorBuilder;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
public class HttpClientConnectorBuilder extends AbstractConnectorBuilder<HttpClientConnectorBuilder> {
    protected static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30L);
    // protected static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30L);

    private Duration connectTimeout;
    // private SSLContext sslContext;

    @Override
    public Connector build() throws Exception {
        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Objects.requireNonNullElse(connectTimeout, DEFAULT_CONNECT_TIMEOUT))
                // .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.ALWAYS);

        // if (authenticator != null) {
        //     httpClientBuilder = httpClientBuilder.authenticator(authenticator);
        // }

        return new JreHttpClientConnector(httpClientBuilder.build());
    }

    public HttpClientConnectorBuilder connectTimeout(final Duration connectTimeout) {
        this.connectTimeout = connectTimeout;

        return self();
    }

    // public HttpClientConnectorBuilder sslContext(final SSLContext sslContext) {
    //     this.sslContext = sslContext;
    //
    //     return self();
    // }

    @Override
    protected HttpClientConnectorBuilder self() {
        return this;
    }
}
