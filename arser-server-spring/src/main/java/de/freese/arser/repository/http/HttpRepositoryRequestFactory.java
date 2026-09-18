package de.freese.arser.repository.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;

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
public final class HttpRepositoryRequestFactory extends AbstractHttpRepository {
    public static Repository of(final HttpRepositoryConfig config) {
        final Repository repository = new HttpRepositoryRequestFactory(config);

        return configure(repository, config);
    }

    private ClientHttpRequestFactory clientHttpRequestFactory;
    private HttpClient httpClient;

    private HttpRepositoryRequestFactory(final HttpRepositoryConfig config) {
        super(config);
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        try {
            final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(remoteUri, HttpMethod.GET);
            clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));
            clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_ACCEPT, List.of(ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM));

            try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
                if (clientHttpResponse.getStatusCode().value() == ArserUtils.HTTP_STATUS_OK) {
                    try (InputStream inputStream = clientHttpResponse.getBody()) {
                        return new ArserResult.Download(DefaultBlobValue.of(inputStream));
                    }
                }

                try (InputStream inputStream = clientHttpResponse.getBody()) {
                    // Drain the Body.
                    inputStream.transferTo(OutputStream.nullOutputStream());
                }

                return new ArserResult.NotFound(remoteUri);
            }
        }
        catch (final Exception ex) {
            throw new RepositoryException(ex);
        }
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        try {
            final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(remoteUri, HttpMethod.HEAD);
            clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));

            final int responseCode;

            try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
                responseCode = clientHttpResponse.getStatusCode().value();
            }

            if (responseCode == ArserUtils.HTTP_STATUS_OK) {
                return new ArserResult.Exist(remoteUri);
            }

            return new ArserResult.NotFound(remoteUri);
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

        clientHttpRequestFactory = new JdkClientHttpRequestFactory(httpClient);
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        clientHttpRequestFactory = null;

        // httpClient.close();
        httpClient.shutdownNow();
        httpClient = null;
    }
}
