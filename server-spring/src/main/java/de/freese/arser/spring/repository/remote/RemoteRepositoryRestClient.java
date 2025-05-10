// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.springframework.web.client.RestClient;

import de.freese.arser.core.model.DefaultFileResource;
import de.freese.arser.core.model.FileResource;
import de.freese.arser.core.model.ResourceRequest;
import de.freese.arser.core.repository.AbstractRemoteRepository;
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
    protected boolean doExist(final ResourceRequest resourceRequest) {
        final URI remoteUri = createRemoteUri(getBaseUri(), resourceRequest.getResource());

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
    protected FileResource doGetResource(final ResourceRequest resourceRequest) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), resourceRequest.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("RequestResource: {}", remoteUri);
        }

        return restClient.get()
                .uri(remoteUri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .header(ArserUtils.HTTP_HEADER_ACCEPT, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                .exchange((clientRequest, clientResponse) -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().warn("HTTP-STATUS: {} for {}", clientResponse.getStatusCode().value(), remoteUri);
                    }

                    if (clientResponse.getStatusCode().value() != ArserUtils.HTTP_STATUS_OK) {
                        try (InputStream inputStream = clientResponse.getBody()) {
                            // Drain the Body.
                            inputStream.transferTo(OutputStream.nullOutputStream());
                        }

                        return null;
                    }

                    final long contentLength = clientResponse.getHeaders().getContentLength();
                    final Path path;

                    try (InputStream inputStream = clientResponse.getBody()) {
                        path = writeToTempFile(resourceRequest.getResource(), inputStream);
                    }

                    return new DefaultFileResource(contentLength, () ->
                            new FilterInputStream(Files.newInputStream(path)) {
                                @Override
                                public void close() throws IOException {
                                    super.close();

                                    // Delete Temp-File.
                                    try {
                                        Files.delete(path);
                                    }
                                    catch (IOException ex) {
                                        getLogger().error(ex.getMessage(), ex);
                                    }
                                }
                            });
                });
    }
}
