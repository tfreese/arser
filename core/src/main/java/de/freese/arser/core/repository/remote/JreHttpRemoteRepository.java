// Created: 22.07.23
package de.freese.arser.core.repository.remote;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

import de.freese.arser.core.repository.RepositoryResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpRemoteRepository extends AbstractRemoteRepository {

    private final Supplier<HttpClient> httpClientSupplier;

    public JreHttpRemoteRepository(final String name, final URI uri, final Supplier<HttpClient> httpClientSupplier) {
        super(name, uri);

        this.httpClientSupplier = checkNotNull(httpClientSupplier, "Supplier<HttpClient>");
    }

    @Override
    protected boolean doExist(final URI resource) throws Exception {
        final URI uri = createResourceUri(getUri(), resource);

        // @formatter:off
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build()
                ;
        // @formatter:on

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", request);
        }

        final HttpResponse<Void> response = getHttpClient().send(request, HttpResponse.BodyHandlers.discarding());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {}", response);
        }

        return response.statusCode() == ArserUtils.HTTP_OK;
    }

    @Override
    protected RepositoryResponse doGetInputStream(final URI resource) throws Exception {
        final URI uri = createResourceUri(getUri(), resource);

        // @formatter:off
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .GET()
                .build()
                ;
        // @formatter:on

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - Request: {}", request);
        }

        final HttpResponse<InputStream> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - Response: {}", response);
        }

        if (response.statusCode() != ArserUtils.HTTP_OK) {
            return null;
        }

        final long contentLength = response.headers().firstValueAsLong(ArserUtils.HTTP_HEADER_CONTENT_LENGTH).orElse(0);

        return new RepositoryResponse(uri, contentLength, response.body());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        checkNotNull(httpClientSupplier, "HttpClientSupplier");
    }

    protected HttpClient getHttpClient() {
        return httpClientSupplier.get();
    }
}
