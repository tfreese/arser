package de.freese.arser.repository.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.DefaultBlobValue;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 * @since 22.08.26
 */
public class HttpRepositoryRequestFactory extends AbstractHttpRepository {
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public HttpRepositoryRequestFactory(final HttpRepositoryConfig config, final ClientHttpRequestFactory clientHttpRequestFactory) {
        super(config);

        this.clientHttpRequestFactory = Objects.requireNonNull(clientHttpRequestFactory, "clientHttpRequestFactory required");
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
            return new ArserResult.Failure(ex);
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
            return new ArserResult.Failure(ex);
        }
    }
}
