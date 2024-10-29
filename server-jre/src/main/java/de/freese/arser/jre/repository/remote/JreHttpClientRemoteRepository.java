// Created: 22.07.23
package de.freese.arser.jre.repository.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import de.freese.arser.core.repository.remote.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceHandle;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpClientRemoteRepository extends AbstractRemoteRepository {
    private final Supplier<HttpClient> httpClientSupplier;

    private HttpClient httpClient;

    public JreHttpClientRemoteRepository(final String name, final URI uri, final Supplier<HttpClient> httpClientSupplier, final Path tempDir) {
        super(name, uri, tempDir);

        this.httpClientSupplier = assertNotNull(httpClientSupplier, () -> "Supplier<HttpClient>");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .method("HEAD", HttpRequest.BodyPublishers.noBody()) // Liefert Header, Status und ResponseBody.
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", httpRequest);
        }

        final HttpResponse<Void> httpResponse = getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.discarding());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {}", httpResponse);
        }

        return httpResponse.statusCode() == ArserUtils.HTTP_OK;
    }

    @Override
    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .GET()
                .build();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Request: {}", httpRequest);
        }

        final HttpResponse<InputStream> httpResponse = getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Response: {}", httpResponse);
        }

        if (httpResponse.statusCode() != ArserUtils.HTTP_OK) {
            return null;
        }

        final long contentLength = httpResponse.headers().firstValueAsLong(ArserUtils.HTTP_HEADER_CONTENT_LENGTH).orElse(-1);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
        }

        final ResourceHandle resourceHandle;

        try (InputStream inputStream = httpResponse.body()) {
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

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        assertNotNull(httpClientSupplier, () -> "HttpClientSupplier");

        httpClient = httpClientSupplier.get();

        // final ResilientHttpClient.ResilientHttpClientBuilder resilientHttpClientBuilder = ResilientHttpClient.newBuilder(httpClientSupplier.get())
        //         .retries(2)
        //         .retryDuration(Duration.ofMillis(750));
        //
        // httpClient = resilientHttpClientBuilder.build();
    }

    protected HttpClient getHttpClient() {
        return httpClient;
    }
}
