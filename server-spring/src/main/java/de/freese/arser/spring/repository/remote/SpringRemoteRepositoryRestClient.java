// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResponseHandler;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringRemoteRepositoryRestClient extends AbstractRemoteRepository {
    private final RestClient restClient;

    public SpringRemoteRepositoryRestClient(final String name, final URI uri, final RestClient restClient) {
        super(name, uri);

        this.restClient = Objects.requireNonNull(restClient, "restClient required");
    }

    @Override
    protected boolean doExist(final ResourceRequest resourceRequest) {
        final URI remoteUri = createRemoteUri(getUri(), resourceRequest.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", remoteUri);
        }

        return Boolean.TRUE.equals(restClient.head()
                .uri(remoteUri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .exchange((clientRequest, clientResponse) -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("exist - Response: {} / {}", clientResponse.getStatusCode(), remoteUri);
                    }

                    return clientResponse.getStatusCode().is2xxSuccessful();
                })
        );
    }

    @Override
    protected void doStreamTo(final ResourceRequest resourceRequest, final ResponseHandler handler) {
        final URI remoteUri = createRemoteUri(getUri(), resourceRequest.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Request: {}", remoteUri);
        }

        // Note: The response is closed after the exchange function has been invoked.
        restClient.get()
                .uri(remoteUri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange((clientRequest, clientResponse) -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Resource - Response: {} / {}", clientResponse.getStatusCode(), remoteUri);
                    }

                    if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                        try (InputStream inputStream = clientResponse.getBody()) {
                            inputStream.transferTo(OutputStream.nullOutputStream());
                        }

                        final String message = "HTTP-STATUS: %d for %s".formatted(clientResponse.getStatusCode().value(), remoteUri.toString());
                        handler.onError(new IOException(message));

                        return null;
                    }

                    final long contentLength = clientResponse.getHeaders().getContentLength();

                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), remoteUri);
                    }

                    try (InputStream inputStream = clientResponse.getBody()) {
                        handler.onSuccess(contentLength, inputStream);
                    }

                    return null;
                });
    }
}
