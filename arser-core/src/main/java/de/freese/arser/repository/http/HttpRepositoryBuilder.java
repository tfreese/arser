package de.freese.arser.repository.http;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

import de.freese.arser.connector.decorator.CachingConnector;
import de.freese.arser.connector.decorator.LoggingConnector;
import de.freese.arser.connector.decorator.RetryingConnector;
import de.freese.arser.connector.http.JreHttpClientConnector;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.repository.AbstractRepositoryBuilder;

/**
 * @author Thomas Freese
 */
public final class HttpRepositoryBuilder extends AbstractRepositoryBuilder<HttpRepositoryBuilder, HttpRepository> {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30L);
    // private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30L);

    private Duration cachingTtl;
    private Duration connectTimeout;
    private int maxRetries = 3;
    private Duration retryInterval = Duration.ofSeconds(3L);

    // private SSLContext sslContext;

    HttpRepositoryBuilder() {
        super();
    }

    @Override
    public HttpRepository build() throws Exception {
        Objects.requireNonNull(getUri(), "URI required");
        Objects.requireNonNull(getName(), "name required");

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Objects.requireNonNullElse(connectTimeout, DEFAULT_CONNECT_TIMEOUT))
                // .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.ALWAYS);

        // if (authenticator != null) {
        //     httpClientBuilder = httpClientBuilder.authenticator(authenticator);
        // }

        Connector connector = new JreHttpClientConnector(httpClientBuilder.build());

        if (maxRetries > 0 && retryInterval.isPositive()) {
            connector = new RetryingConnector(connector, maxRetries, retryInterval);
        }

        if (cachingTtl != null && cachingTtl.isPositive()) {
            connector = new CachingConnector(connector, cachingTtl);
        }

        if (isLogging()) {
            connector = new LoggingConnector(connector);
        }

        return new HttpRepository(getUri(), getName(), connector);
    }

    public HttpRepositoryBuilder connectTimeout(final Duration connectTimeout) {
        this.connectTimeout = connectTimeout;

        return self();
    }

    public HttpRepositoryBuilder withCaching(final Duration ttl) {
        this.cachingTtl = ttl;

        return self();
    }

    public HttpRepositoryBuilder withRetrying(final int maxRetries, final Duration retryInterval) {
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;

        return self();
    }

    // public HttpClientConnectorBuilder sslContext(final SSLContext sslContext) {
    //     this.sslContext = sslContext;
    //
    //     return self();
    // }

    @Override
    protected HttpRepositoryBuilder self() {
        return this;
    }
}
