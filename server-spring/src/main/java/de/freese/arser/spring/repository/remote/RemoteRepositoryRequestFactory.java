// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResponseHandler;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class RemoteRepositoryRequestFactory extends AbstractRemoteRepository {
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public RemoteRepositoryRequestFactory(final String name, final URI uri, final ClientHttpRequestFactory clientHttpRequestFactory) {
        super(name, uri);

        this.clientHttpRequestFactory = Objects.requireNonNull(clientHttpRequestFactory, "clientHttpRequestFactory required");
    }

    @Override
    protected boolean doExist(final ResourceRequest resourceRequest) throws Exception {
        final URI remoteUri = createRemoteUri(getUri(), resourceRequest.getResource());

        final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(remoteUri, HttpMethod.HEAD);
        clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));

        final int responseCode;

        try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
            responseCode = clientHttpResponse.getStatusCode().value();
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {} / {}", responseCode, remoteUri);
        }

        return responseCode == ArserUtils.HTTP_STATUS_OK;
    }

    @Override
    protected void doStreamTo(final ResourceRequest resourceRequest, final ResponseHandler handler) throws Exception {
        final URI remoteUri = createRemoteUri(getUri(), resourceRequest.getResource());

        final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(remoteUri, HttpMethod.GET);
        clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));
        clientHttpRequest.getHeaders().put("Accept", List.of(MediaType.APPLICATION_OCTET_STREAM_VALUE));

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Request: {}", remoteUri);
        }

        try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
            final int responseCode = clientHttpResponse.getStatusCode().value();

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Resource - Response: {} / {}", responseCode, remoteUri);
            }

            if (responseCode != ArserUtils.HTTP_STATUS_OK) {
                // Drain Body.
                try (InputStream inputStream = clientHttpResponse.getBody()) {
                    inputStream.transferTo(OutputStream.nullOutputStream());
                }

                final String message = "HTTP-STATUS: %d for %s".formatted(responseCode, remoteUri.toString());
                handler.onError(new IOException(message));

                return;
            }

            final long contentLength = clientHttpResponse.getHeaders().getContentLength();

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), remoteUri);
            }

            try (InputStream inputStream = clientHttpResponse.getBody()) {
                handler.onSuccess(contentLength, inputStream);
            }
        }
    }
}
