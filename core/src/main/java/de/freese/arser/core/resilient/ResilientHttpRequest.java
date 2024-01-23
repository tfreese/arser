// Created: 23.01.24
package de.freese.arser.core.resilient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Thomas Freese
 */
class ResilientHttpRequest extends HttpRequest {
    private final HttpRequest httpRequest;
    private final Supplier<String> loadBalancer;

    ResilientHttpRequest(final HttpRequest httpRequest, final Supplier<String> loadBalancer) {
        super();

        this.httpRequest = Objects.requireNonNull(httpRequest, "httpRequest required");
        this.loadBalancer = Objects.requireNonNull(loadBalancer, "loadBalancer required");
    }

    @Override
    public Optional<BodyPublisher> bodyPublisher() {
        return httpRequest.bodyPublisher();
    }

    @Override
    public boolean expectContinue() {
        return httpRequest.expectContinue();
    }

    @Override
    public HttpHeaders headers() {
        return httpRequest.headers();
    }

    @Override
    public String method() {
        return httpRequest.method();
    }

    @Override
    public Optional<Duration> timeout() {
        return httpRequest.timeout();
    }

    @Override
    public String toString() {
        return httpRequest.toString();
    }

    @Override
    public URI uri() {
        final URI uri = httpRequest.uri();
        final String hostName = loadBalancer.get();

        if (hostName == null) {
            return uri;
        }

        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), hostName, uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        }
        catch (URISyntaxException ex) {
            throw new IllegalStateException("Can not build uri " + uri + "with address " + hostName, ex);
        }
    }

    @Override
    public Optional<HttpClient.Version> version() {
        return httpRequest.version();
    }
}
