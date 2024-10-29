// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import de.freese.arser.core.repository.remote.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceHandle;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringRemoteRepositoryRestClient extends AbstractRemoteRepository {
    private final RestClient restClient;

    public SpringRemoteRepositoryRestClient(final String name, final URI uri, final RestClient restClient, final Path tempDir) {
        super(name, uri, tempDir);

        this.restClient = assertNotNull(restClient, () -> "restClient");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", uri);
        }

        return restClient.head()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .exchange((clientRequest, clientResponse) -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("exist - Response: {} / {}", clientResponse.getStatusCode(), uri);
                    }

                    return clientResponse.getStatusCode().is2xxSuccessful();
                });
    }

    @Override
    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Request: {}", uri);
        }

        // Note: The response is closed after the exchange function has been invoked
        return restClient.head()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange((clientRequest, clientResponse) -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Resource - Response: {} / {}", clientResponse.getStatusCode(), uri);
                    }

                    if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                        return null;
                    }

                    final long contentLength = clientResponse.getHeaders().getContentLength();

                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
                    }

                    final ResourceHandle resourceHandle;

                    try (InputStream inputStream = clientResponse.getBody()) {
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
                    }

                    return new DefaultResourceResponse(contentLength, resourceHandle);
                });
    }
}
