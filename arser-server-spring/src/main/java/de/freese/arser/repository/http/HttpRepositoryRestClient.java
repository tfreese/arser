package de.freese.arser.repository.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

import org.springframework.web.client.RestClient;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.DefaultBlobValue;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 * @since 22.08.26
 */
public class HttpRepositoryRestClient extends AbstractHttpRepository {
    private final RestClient restClient;

    public HttpRepositoryRestClient(final HttpRepositoryConfig config, final RestClient restClient) {
        super(config);

        this.restClient = Objects.requireNonNull(restClient, "restClient required");
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
            return new ArserResult.Failure(ex);
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
            return new ArserResult.Failure(ex);
        }
    }
}
