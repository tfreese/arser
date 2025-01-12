// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.net.URI;
import java.util.Objects;

import org.springframework.web.client.RestClient;

import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class RemoteRepositoryRestClient extends AbstractRemoteRepository {
    private final RestClient restClient;

    public RemoteRepositoryRestClient(final String name, final URI baseUri, final RestClient restClient) {
        super(name, baseUri);

        this.restClient = Objects.requireNonNull(restClient, "restClient required");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) {
        final URI remoteUri = createRemoteUri(getBaseUri(), request.getResource());

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
}
