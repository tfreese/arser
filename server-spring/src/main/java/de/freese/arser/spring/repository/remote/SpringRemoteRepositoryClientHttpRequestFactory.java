// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceHandle;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringRemoteRepositoryClientHttpRequestFactory extends AbstractRemoteRepository {
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public SpringRemoteRepositoryClientHttpRequestFactory(final String name, final URI uri, final ClientHttpRequestFactory clientHttpRequestFactory, final Path tempDir) {
        super(name, uri, tempDir);

        this.clientHttpRequestFactory = Objects.requireNonNull(clientHttpRequestFactory, "clientHttpRequestFactory required");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createRemoteUri(getUri(), request.getResource());

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
    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        final URI uri = createRemoteUri(getUri(), request.getResource());

        final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
        clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));
        clientHttpRequest.getHeaders().put("Accept", List.of(MediaType.APPLICATION_OCTET_STREAM_VALUE));

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Request: {}", uri);
        }

        try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
            final int responseCode = clientHttpResponse.getStatusCode().value();

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Resource - Response: {} / {}", responseCode, uri);
            }

            if (responseCode != ArserUtils.HTTP_OK) {
                return null;
            }

            final long contentLength = clientHttpResponse.getHeaders().getContentLength();

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
            }

            final ResourceHandle resourceHandle;

            try (InputStream inputStream = clientHttpResponse.getBody()) {
                if (contentLength > 0 && contentLength < KEEP_IN_MEMORY_LIMIT) {
                    // Keep small files in Memory.
                    final byte[] bytes = inputStream.readAllBytes();

                    resourceHandle = () -> new ByteArrayInputStream(bytes);
                }
                else {
                    // Use Temp-Files.
                    final Path tempFile = saveTemp(inputStream);

                    resourceHandle = new ResourceHandle() {
                        @Override
                        public void close() {
                            try {
                                Files.delete(tempFile);
                            }
                            catch (IOException ex) {
                                getLogger().error(ex.getMessage(), ex);
                            }
                        }

                        @Override
                        public InputStream createInputStream() throws IOException {
                            return Files.newInputStream(tempFile);
                        }
                    };
                }

                return new DefaultResourceResponse(contentLength, resourceHandle);
            }
        }
    }
}
