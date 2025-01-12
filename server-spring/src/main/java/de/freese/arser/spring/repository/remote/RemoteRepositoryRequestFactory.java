// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class RemoteRepositoryRequestFactory extends AbstractRemoteRepository {
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public RemoteRepositoryRequestFactory(final String name, final URI baseUri, final ClientHttpRequestFactory clientHttpRequestFactory) {
        super(name, baseUri);

        this.clientHttpRequestFactory = Objects.requireNonNull(clientHttpRequestFactory, "clientHttpRequestFactory required");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), request.getResource());

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
}
