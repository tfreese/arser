// Created: 23.01.24
package de.freese.arser.core.resilient;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RetryPolicy;
import dev.failsafe.function.CheckedSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sendAsync: HttpResponse.PushPromiseHandler is ignored, when retries are set !
 *
 * @author Thomas Freese
 */
public class ResilientHttpClient extends HttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientHttpClient.class);

    public static class ResilientHttpClientBuilder {
        private HttpClient httpClient;
        private Supplier<String> loadBalancer = () -> null;
        private int retries;
        private Duration retryDuration = Duration.ofSeconds(1);

        public HttpClient build() {
            Objects.requireNonNull(httpClient, "httpClient required");

            FailsafeExecutor<HttpResponse<?>> failsafeExecutor = null;

            if (retries > 0) {
                final RetryPolicy<HttpResponse<?>> retryPolicy = RetryPolicy.<HttpResponse<?>>builder()
                        // .withMaxAttempts(3) // Overall Try's.
                        .withMaxRetries(retries) // Try's after first call.
                        .withDelay(retryDuration).onRetry(event -> {
                            final Throwable lastException = event.getLastException();

                            if (lastException instanceof HttpRetryException httpRetryException) {
                                LOGGER.error("Retry: {} - HTTP {} - {}", event.getExecutionCount(), httpRetryException.responseCode(), httpRetryException.getMessage());
                            }
                            else if (lastException != null) {
                                LOGGER.error("Retry: {} - - {}", event.getExecutionCount(), lastException.getMessage());
                            }
                            else {
                                LOGGER.error("Retry: {}", event.getExecutionCount());
                            }
                        }).build();

                failsafeExecutor = Failsafe.with(retryPolicy);
            }

            return new ResilientHttpClient(httpClient, loadBalancer, failsafeExecutor);
        }

        public ResilientHttpClientBuilder httpClient(final HttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient, "httpClient required");

            return this;
        }

        public ResilientHttpClientBuilder httpClientBuilder(final HttpClient.Builder httpClientBuilder) {
            Objects.requireNonNull(httpClientBuilder, "httpClientBuilder required");

            if (this.httpClient != null) {
                this.httpClient.close();
            }

            this.httpClient = httpClientBuilder.build();

            return this;
        }

        public ResilientHttpClientBuilder loadBalancer(final Supplier<String> loadBalancer) {
            this.loadBalancer = Objects.requireNonNull(loadBalancer, "loadBalancer required");

            return this;
        }

        public ResilientHttpClientBuilder retries(final int retries) {
            this.retries = retries;

            return this;
        }

        public ResilientHttpClientBuilder retryDuration(final Duration retryDuration) {
            this.retryDuration = Objects.requireNonNull(retryDuration, "retryDuration required");

            return this;
        }
    }

    public static ResilientHttpClientBuilder newBuilder(final HttpClient httpClient) {
        return new ResilientHttpClientBuilder().httpClient(httpClient);
    }

    private final FailsafeExecutor<HttpResponse<?>> failsafeExecutor;
    private final HttpClient httpClient;
    private final Supplier<String> loadBalancer;

    ResilientHttpClient(final HttpClient httpClient, final Supplier<String> loadBalancer, final FailsafeExecutor<HttpResponse<?>> failsafeExecutor) {
        super();

        this.httpClient = Objects.requireNonNull(httpClient, "httpClient required");
        this.loadBalancer = Objects.requireNonNull(loadBalancer, "loadBalancer required");
        this.failsafeExecutor = failsafeExecutor;
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return httpClient.authenticator();
    }

    @Override
    public boolean awaitTermination(final Duration duration) throws InterruptedException {
        return httpClient.awaitTermination(duration);
    }

    @Override
    public void close() {
        httpClient.close();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return httpClient.connectTimeout();
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return httpClient.cookieHandler();
    }

    @Override
    public Optional<Executor> executor() {
        return httpClient.executor();
    }

    @Override
    public Redirect followRedirects() {
        return httpClient.followRedirects();
    }

    @Override
    public boolean isTerminated() {
        return httpClient.isTerminated();
    }

    @Override
    public WebSocket.Builder newWebSocketBuilder() {
        return httpClient.newWebSocketBuilder();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return httpClient.proxy();
    }

    @Override
    public <T> HttpResponse<T> send(final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        if (this.failsafeExecutor == null) {
            return httpClient.send(request, responseBodyHandler);
        }

        final CheckedSupplier<HttpResponse<T>> checkedSupplier = () -> {
            final HttpResponse<T> response = httpClient.send(new ResilientHttpRequest(request, loadBalancer), responseBodyHandler);

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new HttpRetryException(response.uri().toString(), response.statusCode());
            }

            return response;
        };

        return this.failsafeExecutor.get(checkedSupplier);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return sendAsync(request, responseBodyHandler, null);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler,
                                                            final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        if (this.failsafeExecutor == null) {
            return httpClient.sendAsync(request, responseBodyHandler, pushPromiseHandler);
        }

        final Supplier<HttpResponse<T>> supplier = () -> {
            try {
                return send(request, responseBodyHandler);
            }
            catch (RuntimeException ex) {
                throw ex; // Also avoids double wrapping CompletionExceptions below.
            }
            catch (Exception ex) {
                throw new CompletionException(ex);
            }
        };

        final Executor executor = executor().orElse(null);

        if (executor != null) {
            return CompletableFuture.supplyAsync(supplier, executor);
        }

        return CompletableFuture.supplyAsync(supplier);
    }

    @Override
    public void shutdown() {
        httpClient.shutdown();
    }

    @Override
    public void shutdownNow() {
        httpClient.shutdownNow();
    }

    @Override
    public SSLContext sslContext() {
        return httpClient.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return httpClient.sslParameters();
    }

    @Override
    public String toString() {
        return httpClient.toString();
    }

    @Override
    public Version version() {
        return httpClient.version();
    }
}
