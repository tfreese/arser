package de.freese.arser.core.repository.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import de.freese.arser.core.model.ArserRequest;
import de.freese.arser.core.model.ArserResult;
import de.freese.arser.core.model.BlobValue;
import de.freese.arser.core.repository.AbstractRepository;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class HttpRepository extends AbstractRepository {
    private HttpClient httpClient;

    public HttpRepository(final URI uri, final String name, final HttpClient httpClient) {
        super(uri, name);

        this.httpClient = Objects.requireNonNull(httpClient, "httpClient required");
    }

    @Override
    public <R> ArserResult<R> exist(final ArserRequest arserRequest) {
        final URI remoteUri = arserRequest.toRemoteUri(getUri());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .HEAD()
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Request: {}", httpRequest);
        }

        try {
            final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Response: {}", httpResponse);
            }

            return new ArserResult.Exist<>(httpResponse.statusCode() == ArserUtils.HTTP_STATUS_OK);
        }
        catch (final IOException | InterruptedException ex) {
            return new ArserResult.Failure<>(ex);
        }
    }

    @Override
    public <R> ArserResult<R> getResource(final ArserRequest arserRequest) {
        final URI remoteUri = arserRequest.toRemoteUri(getUri());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .GET()
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .header(ArserUtils.HTTP_HEADER_ACCEPT, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Request: {}", httpRequest);
        }

        try {
            final HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Response: {}", httpResponse);
            }

            if (httpResponse.statusCode() != ArserUtils.HTTP_STATUS_OK) {
                getLogger().warn("HTTP-STATUS: {} for {}", httpResponse.statusCode(), remoteUri);

                try (InputStream inputStream = httpResponse.body()) {
                    // Drain the Body.
                    inputStream.transferTo(OutputStream.nullOutputStream());
                }

                return new ArserResult.NotFound<>(remoteUri);
            }

            final long contentLength = httpResponse.headers().firstValueAsLong(ArserUtils.HTTP_HEADER_CONTENT_LENGTH).orElse(-1);

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), remoteUri);
            }

            return new ArserResult.Resource<>(BlobValue.of(httpResponse.body()));
        }
        catch (final IOException | InterruptedException ex) {
            return new ArserResult.Failure<>(ex);
        }
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
