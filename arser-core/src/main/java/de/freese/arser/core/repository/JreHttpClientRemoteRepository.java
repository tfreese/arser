// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResponseHandler;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpClientRemoteRepository extends AbstractRemoteRepository {

    private HttpClient httpClient;

    public JreHttpClientRemoteRepository(final String contextRoot, final URI uri) {
        super(contextRoot, uri);
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI remoteUri = createRemoteUri(getUri(), request.getResource());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .method("HEAD", HttpRequest.BodyPublishers.noBody()) // Liefert Header, Status und ResponseBody.
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", httpRequest);
        }

        final HttpResponse<Void> httpResponse = getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.discarding());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {}", httpResponse);
        }

        return httpResponse.statusCode() == ArserUtils.HTTP_OK;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(30));

        httpClient = httpClientBuilder.build();

        // final ResilientHttpClient.ResilientHttpClientBuilder resilientHttpClientBuilder = ResilientHttpClient.newBuilder(httpClient)
        //         .retries(2)
        //         .retryDuration(Duration.ofMillis(750));
        //
        // httpClient = resilientHttpClientBuilder.build();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        httpClient.close();
        httpClient = null;
    }

    @Override
    protected void doStreamTo(final ResourceRequest resourceRequest, final ResponseHandler handler) throws Exception {
        final URI remoteUri = createRemoteUri(getUri(), resourceRequest.getResource());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .header("Accept", "application/octet-stream")
                .GET()
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Request: {}", httpRequest);
        }

        final HttpResponse<InputStream> httpResponse = getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Response: {}", httpResponse);
        }

        if (httpResponse.statusCode() != ArserUtils.HTTP_OK) {
            try (InputStream inputStream = httpResponse.body()) {
                inputStream.transferTo(OutputStream.nullOutputStream());
            }

            final String message = "HTTP-STATUS: %d for %s".formatted(httpResponse.statusCode(), remoteUri.toString());
            handler.onError(new IOException(message));

            return;
        }

        final long contentLength = httpResponse.headers().firstValueAsLong(ArserUtils.HTTP_HEADER_CONTENT_LENGTH).orElse(-1);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), remoteUri);
        }

        try (InputStream inputStream = httpResponse.body()) {
            handler.onSuccess(contentLength, inputStream);
        }
    }

    protected HttpClient getHttpClient() {
        return httpClient;
    }
}
