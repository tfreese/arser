package de.freese.arser.repository.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

import de.freese.arser.connector.decorator.RetryingConnector;
import de.freese.arser.connector.http.JreHttpClientConnector;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.utils.AbstractBuilder;

/**
 * @author Thomas Freese
 */
public final class HttpRepositoryBuilder extends AbstractBuilder<HttpRepositoryBuilder, HttpRepository> {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30L);
    // private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30L);

    private Duration connectTimeout;
    private String name;
    private URI uri;
    // private SSLContext sslContext;

    HttpRepositoryBuilder() {
        super();
    }

    @Override
    public HttpRepository build() throws Exception {
        Objects.requireNonNull(uri, "URI required");
        Objects.requireNonNull(name, "name required");

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Objects.requireNonNullElse(connectTimeout, DEFAULT_CONNECT_TIMEOUT))
                // .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.ALWAYS);

        // if (authenticator != null) {
        //     httpClientBuilder = httpClientBuilder.authenticator(authenticator);
        // }

        final Connector httpConnector = new JreHttpClientConnector(httpClientBuilder.build());
        final Connector httpResilientConnector = new RetryingConnector(httpConnector, 3, Duration.ofSeconds(3L));

        return new HttpRepository(uri, name, httpResilientConnector);
    }

    public HttpRepositoryBuilder connectTimeout(final Duration connectTimeout) {
        this.connectTimeout = connectTimeout;

        return self();
    }

    // public HttpClientConnectorBuilder sslContext(final SSLContext sslContext) {
    //     this.sslContext = sslContext;
    //
    //     return self();
    // }

    public HttpRepositoryBuilder name(final String name) {
        this.name = name;

        return self();
    }

    public HttpRepositoryBuilder uri(final URI uri) {
        this.uri = uri;

        return self();
    }

    @Override
    protected HttpRepositoryBuilder self() {
        return this;
    }
}
