// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import de.freese.arser.core.model.DefaultFileResource;
import de.freese.arser.core.model.FileResource;
import de.freese.arser.core.model.ResourceRequest;
import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class RemoteRepositoryRequestFactory extends AbstractRemoteRepository {
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public RemoteRepositoryRequestFactory(final String name, final URI baseUri, final Path workingDir, final ClientHttpRequestFactory clientHttpRequestFactory) {
        super(name, baseUri, workingDir);

        this.clientHttpRequestFactory = Objects.requireNonNull(clientHttpRequestFactory, "clientHttpRequestFactory required");
    }

    @Override
    protected boolean doExist(final ResourceRequest resourceRequest) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), resourceRequest.getResource());

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
    protected FileResource doGetResource(final ResourceRequest resourceRequest) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), resourceRequest.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("RequestResource: {}", remoteUri);
        }

        final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(remoteUri, HttpMethod.GET);
        clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));
        clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_ACCEPT, List.of(ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM));

        final long contentLength;
        final Path path;

        try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
            getLogger().warn("HTTP-STATUS: {} for {}", clientHttpResponse.getStatusCode(), remoteUri);

            if (clientHttpResponse.getStatusCode().value() != ArserUtils.HTTP_STATUS_OK) {
                try (InputStream inputStream = clientHttpResponse.getBody()) {
                    // Drain the Body.
                    inputStream.transferTo(OutputStream.nullOutputStream());
                }

                return null;
            }

            contentLength = clientHttpResponse.getHeaders().getContentLength();

            try (InputStream inputStream = clientHttpResponse.getBody()) {
                path = writeToTempFile(getWorkingDir(), resourceRequest.getResource(), inputStream);
            }
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
    }
}
