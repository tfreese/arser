// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.InputStream;
import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import de.freese.arser.core.repository.remote.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringRemoteRepositoryRestClient extends AbstractRemoteRepository {
    private final RestClient restClient;

    public SpringRemoteRepositoryRestClient(final String name, final URI uri, final RestClient restClient) {
        super(name, uri);

        this.restClient = assertNotNull(restClient, () -> "restClient");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        final ResponseEntity<Void> responseEntity = restClient.head()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .retrieve()
                .toBodilessEntity();

        assert responseEntity != null;

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {} / {}", responseEntity, uri);
        }

        return responseEntity.getStatusCode().is2xxSuccessful();
    }

    @Override
    protected ResourceResponse doGetInputStream(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        // Doesn't work!
        // Note: The response is closed after the exchange function has been invoked
        final ClientHttpResponse clientHttpResponse = restClient.head()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange((clientRequest, clientResponse) -> clientResponse);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - Response: {} / {}", clientHttpResponse.getStatusCode(), uri);
        }

        if (!clientHttpResponse.getStatusCode().is2xxSuccessful()) {
            return null;
        }

        final InputStream inputStream = clientHttpResponse.getBody();

        final long contentLength = clientHttpResponse.getHeaders().getContentLength();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Downloaded {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
        }

        return new DefaultResourceResponse(request, contentLength, inputStream);
    }
}
