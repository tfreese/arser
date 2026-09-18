package de.freese.arser.repository.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Objects;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.DefaultBlobValue;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.RepositoryException;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 * @since 22.08.26
 */
public final class HttpRepositoryRestClient extends AbstractHttpRepository {
    public static Repository of(final HttpRepositoryConfig config, final RestClient.Builder restClientBuilder) {
        final Repository repository = new HttpRepositoryRestClient(config, restClientBuilder);

        return configure(repository, config);
    }

    private final RestClient.Builder restClientBuilder;
    private HttpClient httpClient;
    private RestClient restClient;

    private HttpRepositoryRestClient(final HttpRepositoryConfig config, final RestClient.Builder restClientBuilder) {
        super(config);

        this.restClientBuilder = Objects.requireNonNull(restClientBuilder, "restClientBuilder required");
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        try {
            return restClient.get()
                    .uri(remoteUri)
                    .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                    .header(ArserUtils.HTTP_HEADER_ACCEPT, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                    .exchange((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode().value() == ArserUtils.HTTP_STATUS_OK) {
                            try (InputStream inputStream = clientResponse.getBody()) {
                                return new ArserResult.Download(DefaultBlobValue.of(inputStream));
                            }
                        }

                        try (InputStream inputStream = clientResponse.getBody()) {
                            // Drain the Body.
                            inputStream.transferTo(OutputStream.nullOutputStream());
                        }

                        return new ArserResult.NotFound(remoteUri);
                    });
        }
        catch (final Exception ex) {
            throw new RepositoryException(ex);
        }
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        try {
            return restClient.head()
                    .uri(remoteUri)
                    .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                    .exchange((clientRequest, clientResponse) -> {
                                if (clientResponse.getStatusCode().is2xxSuccessful()) {
                                    return new ArserResult.Exist(remoteUri);
                                }

                                return new ArserResult.NotFound(remoteUri);
                            }
                    );
        }
        catch (final Exception ex) {
            throw new RepositoryException(ex);
        }
    }

    @Override
    public void start() throws Exception {
        super.start();

        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(getConfig().connectTimeout())
                // .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.ALWAYS);

        httpClient = httpClientBuilder.build();

        final ClientHttpRequestFactory clientHttpRequestFactory = new JdkClientHttpRequestFactory(httpClient);

        restClient = restClientBuilder
                .requestFactory(clientHttpRequestFactory)
                .build();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        // httpClient.close();
        httpClient.shutdownNow();
        httpClient = null;

        restClient = null;
    }
}
