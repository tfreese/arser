// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import de.freese.arser.core.repository.remote.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceInfo;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringRemoteRepositoryClientHttpRequestFactory extends AbstractRemoteRepository {
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public SpringRemoteRepositoryClientHttpRequestFactory(final String name, final URI uri, final ClientHttpRequestFactory clientHttpRequestFactory) {
        super(name, uri);

        this.clientHttpRequestFactory = assertNotNull(clientHttpRequestFactory, () -> "clientHttpRequestFactory");
    }

    @Override
    protected ResourceInfo doConsume(final ResourceRequest request, final OutputStream outputStream) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
        clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));

        try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
            final int responseCode = clientHttpResponse.getStatusCode().value();

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("getInputStream - Response: {} / {}", responseCode, uri);
            }

            if (responseCode != ArserUtils.HTTP_OK) {
                return null;
            }

            final long contentLength = clientHttpResponse.getHeaders().getContentLength();

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Downloaded {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
            }

            try (InputStream inputStream = clientHttpResponse.getBody()) {
                inputStream.transferTo(outputStream);
            }

            return new ResourceInfo(request, contentLength);
        }
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(uri, HttpMethod.HEAD);
        clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));

        final int responseCode;

        try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
            responseCode = clientHttpResponse.getStatusCode().value();
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {} / {}", responseCode, uri);
        }

        return responseCode == ArserUtils.HTTP_OK;
    }

    @Override
    protected ResourceResponse doGetInputStream(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
        clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));

        // try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
        final ClientHttpResponse clientHttpResponse = clientHttpRequest.execute();
        final int responseCode = clientHttpResponse.getStatusCode().value();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - Response: {} / {}", responseCode, uri);
        }

        if (responseCode != ArserUtils.HTTP_OK) {
            return null;
        }

        final long contentLength = clientHttpResponse.getHeaders().getContentLength();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Downloaded {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
        }

        return new DefaultResourceResponse(request, contentLength, clientHttpResponse.getBody());
        // }
    }
}
