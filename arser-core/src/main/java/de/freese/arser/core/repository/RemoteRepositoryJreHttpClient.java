// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import de.freese.arser.core.model.DefaultRequestResource;
import de.freese.arser.core.model.RequestResource;
import de.freese.arser.core.model.ResourceRequest;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class RemoteRepositoryJreHttpClient extends AbstractRemoteRepository {

    private HttpClient httpClient;

    public RemoteRepositoryJreHttpClient(final String contextRoot, final URI baseUri) {
        super(contextRoot, baseUri);
    }

    @Override
    protected boolean doExist(final ResourceRequest resourceRequest) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), resourceRequest.getResource());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .HEAD()
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", httpRequest);
        }

        final HttpResponse<Void> httpResponse = getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.discarding());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {}", httpResponse);
        }

        return httpResponse.statusCode() == ArserUtils.HTTP_STATUS_OK;
    }

    @Override
    protected RequestResource doGetResource(final ResourceRequest resourceRequest) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), resourceRequest.getResource());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .GET()
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .header(ArserUtils.HTTP_HEADER_ACCEPT, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("RequestResource: {}", httpRequest);
        }

        final HttpResponse<InputStream> httpResponse = getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("RequestResource: {}", httpResponse);
        }

        if (httpResponse.statusCode() != ArserUtils.HTTP_STATUS_OK) {
            getLogger().warn("HTTP-STATUS: {} for {}", httpResponse.statusCode(), remoteUri);

            try (InputStream inputStream = httpResponse.body()) {
                // Drain the Body.
                inputStream.transferTo(OutputStream.nullOutputStream());
            }

            return null;
        }

        final long contentLength = httpResponse.headers().firstValueAsLong(ArserUtils.HTTP_HEADER_CONTENT_LENGTH).orElse(-1);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), remoteUri);
        }

        return new DefaultRequestResource(contentLength, httpResponse::body);
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

    protected HttpClient getHttpClient() {
        return httpClient;
    }
}
