package de.freese.arser.connector.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Attributes;
import de.freese.arser.connector.security.Credentials;
import de.freese.arser.connector.security.CredentialsProvider;
import de.freese.arser.connector.security.UriGuard;
import de.freese.arser.connector.spi.ConnectorException;
import de.freese.arser.connector.spi.NotFoundException;

/**
 * @author Thomas Freese
 */
public final class HttpConnector extends AbstractHttpConnector {
    private static void applyAuth(final HttpRequest.Builder builder, final Credentials credentials) {
        switch (credentials) {
            case final Credentials.Basic basic -> {
                final String enc = Base64.getEncoder().encodeToString((basic.user() + ":" + basic.password()).getBytes());
                builder.header("Authorization", "Basic " + enc);
            }
            case final Credentials.Bearer bearer -> builder.header("Authorization", "Bearer " + bearer.token());
            case final Credentials.MTLS mtls -> { // Set in HttpClient.sslContext }
            }
        }
    }

    private static void ensure2xx(final int sc, final URI uri) {
        if (sc < 200 || sc >= 300) {
            throw new ConnectorException("Unexpected Status " + sc + " for " + uri);
        }
    }

    private final HttpClient httpClient;

    public HttpConnector(final HttpClient httpClient) {
        this(UriGuard.ALLOW_ALL, CredentialsProvider.NONE, httpClient);
    }

    public HttpConnector(final UriGuard uriGuard, final CredentialsProvider credentialsProvider, final HttpClient httpClient) {
        super(uriGuard, credentialsProvider);

        this.httpClient = Objects.requireNonNull(httpClient, "httpClient required");
    }

    @Override
    public void close() {
        super.close();

        httpClient.close();
    }

    @Override
    protected ConnectorResponse<Void> doDelete(final ConnectorRequest<?> request) {
        final int sc = sendDiscard(baseBuilder(request).DELETE().build());

        if (sc == 404) {
            throw new NotFoundException("404: " + request.uri());
        }

        ensure2xx(sc, request.uri());

        return new ConnectorResponse<>(null, Map.of("statusCode", sc));
    }

    @Override
    protected ConnectorResponse<byte[]> doDownload(final ConnectorRequest<?> request) {
        final HttpResponse<byte[]> response = send(buildGet(request), HttpResponse.BodyHandlers.ofByteArray());

        return toByteArrayResponse(response);
    }

    @Override
    protected ConnectorResponse<InputStream> doDownloadStream(final ConnectorRequest<?> request) {
        final HttpResponse<InputStream> response = send(buildGet(request), HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 404) {
            throw new NotFoundException("404: " + request.uri());
        }

        ensure2xx(response.statusCode(), request.uri());

        return new ConnectorResponse<>(response.body(), Map.of("statusCode", response.statusCode(), "headers", response.headers().map()));
    }

    @Override
    protected ConnectorResponse<Boolean> doExists(final ConnectorRequest<?> request) {
        int statusCode = sendDiscard(buildHead(request));

        // 405 Not Allowed
        // 501 Not Implemented
        if (statusCode == 405 || statusCode == 501) {
            statusCode = sendDiscard(buildGet(request));
        }

        // 404 Not Found
        // 410 Gone
        if (statusCode == 404 || statusCode == 410) {
            return new ConnectorResponse<>(false, Map.of("statusCode", statusCode));
        }

        // 200 OK
        // 300 Multiple Choices Redirection
        if (statusCode >= 200 && statusCode < 300) {
            return new ConnectorResponse<>(true, Map.of("statusCode", statusCode));
        }

        throw new ConnectorException("Unexpected Status " + statusCode + " for " + request.uri());
    }

    @Override
    protected ConnectorResponse<Map<String, List<String>>> doHead(final ConnectorRequest<?> request) {
        final HttpResponse<Void> httpResponse = send(buildHead(request), HttpResponse.BodyHandlers.discarding());

        ensure2xx(httpResponse.statusCode(), request.uri());

        return new ConnectorResponse<>(httpResponse.headers().map(), Map.of("statusCode", httpResponse.statusCode()));
    }

    @Override
    protected ConnectorResponse<Void> doUpload(final ConnectorRequest<?> request) {
        final byte[] body = request.attribute(Attributes.BODY).orElseThrow();

        final String method = request.attributeOrDefault(Attributes.METHOD, "PUT");

        final HttpRequest httpRequest = baseBuilder(request)
                .method(method, HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        final int sc = sendDiscard(httpRequest);

        ensure2xx(sc, request.uri());

        return new ConnectorResponse<>(null, Map.of("statusCode", sc));
    }

    @Override
    protected ConnectorResponse<Long> doUploadStream(final ConnectorRequest<?> request) {
        final Supplier<InputStream> supplier = request.attribute(Attributes.BODY_STREAM).orElseThrow();

        final String method = request.attributeOrDefault(Attributes.METHOD, "PUT");

        final HttpRequest httpRequest = baseBuilder(request)
                .method(method, HttpRequest.BodyPublishers.ofInputStream(supplier))
                .build();

        final int sc = sendDiscard(httpRequest);

        ensure2xx(sc, request.uri());

        return new ConnectorResponse<>(-1L, Map.of("statusCode", sc));
    }

    private HttpRequest.Builder baseBuilder(final ConnectorRequest<?> request) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .timeout(request.attributeOrDefault(Attributes.TIMEOUT, Duration.ofSeconds(5L)));

        request.attribute(Attributes.HEADERS).ifPresent(h -> h.forEach(builder::header));

        getCredentialsProvider().resolve(request.uri()).ifPresent(c -> applyAuth(builder, c));

        return builder;
    }

    private HttpRequest buildGet(final ConnectorRequest<?> request) {
        return baseBuilder(request).GET().build();
    }

    private HttpRequest buildHead(final ConnectorRequest<?> request) {
        return baseBuilder(request).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
    }

    private <T> HttpResponse<T> send(final HttpRequest httpRequest, final HttpResponse.BodyHandler<T> bodyHandler) {
        try {
            return httpClient.send(httpRequest, bodyHandler);
        }
        catch (final IOException ex) {
            throw new ConnectorException("HTTP-IO-Fehler: " + httpRequest.uri(), ex);
        }
        catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new ConnectorException("Unterbrochen: " + httpRequest.uri(), ex);
        }
    }

    private int sendDiscard(final HttpRequest httpRequest) {
        return send(httpRequest, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private ConnectorResponse<byte[]> toByteArrayResponse(final HttpResponse<byte[]> httpResponse) {
        if (httpResponse.statusCode() == 404) {
            throw new NotFoundException("404: " + httpResponse.uri());
        }

        ensure2xx(httpResponse.statusCode(), httpResponse.uri());

        return new ConnectorResponse<>(httpResponse.body(), Map.of("statusCode", httpResponse.statusCode(), "headers", httpResponse.headers().map()));
    }
}
