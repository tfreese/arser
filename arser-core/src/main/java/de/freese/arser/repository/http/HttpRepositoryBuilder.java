package de.freese.arser.repository.http;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.connector.decorator.CachingConnectorDecorator;
import de.freese.arser.connector.decorator.LoggingConnectorDecorator;
import de.freese.arser.connector.decorator.RetryingConnectorDecorator;
import de.freese.arser.connector.http.JreHttpClientConnector;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.repository.AbstractRepositoryBuilder;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.decorator.LoggingRepositoryDecorator;

/**
 * @author Thomas Freese
 */
public final class HttpRepositoryBuilder extends AbstractRepositoryBuilder<HttpRepositoryBuilder, Repository> {
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
    public Repository build(final LifeCycleRegistry lifeCycleRegistry) throws Exception {
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
            connector = new RetryingConnectorDecorator(connector, maxRetries, retryInterval);
        }

        if (cachingTtl != null && cachingTtl.isPositive()) {
            connector = new CachingConnectorDecorator(connector, cachingTtl);
        }

        if (isLogging()) {
            connector = new LoggingConnectorDecorator(connector);
        }

        lifeCycleRegistry.register(connector);

        Repository repository = new HttpRepository(getUri(), getName(), connector);

        if (isLogging()) {
            repository = new LoggingRepositoryDecorator(repository);
        }

        lifeCycleRegistry.register(repository);

        return repository;
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
