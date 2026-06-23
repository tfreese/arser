package de.freese.arser.core.connector.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import de.freese.arser.core.connector.AbstractConnector;
import de.freese.arser.core.model.ArserRequest;
import de.freese.arser.core.model.BlobValue;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpClientConnector extends AbstractConnector {
    private HttpClient httpClient;

    public JreHttpClientConnector(final URI uri, final HttpClient httpClient) {
        super(uri);

        this.httpClient = Objects.requireNonNull(httpClient, "httpClient required");
    }

    @Override
    public boolean exist(final ArserRequest arserRequest) throws Exception {
        final URI remoteUri = arserRequest.toRemoteUri(getUri());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .HEAD()
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", httpRequest);
        }

        final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {}", httpResponse);
        }

        return httpResponse.statusCode() == ArserUtils.HTTP_STATUS_OK;
    }

    @Override
    public BlobValue getResource(final ArserRequest arserRequest) throws Exception {
        final URI remoteUri = arserRequest.toRemoteUri(getUri());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .GET()
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .header(ArserUtils.HTTP_HEADER_ACCEPT, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("RequestResource: {}", httpRequest);
        }

        final HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

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

        return BlobValue.of(httpResponse.body());
    }

    @Override
    protected void doStart() throws Exception {
        // Empty
    }

    @Override
    protected void doStop() throws Exception {
        httpClient.close();
        httpClient = null;
    }
}
