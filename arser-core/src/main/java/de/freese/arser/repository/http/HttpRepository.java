package de.freese.arser.repository.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.DefaultBlobValue;
import de.freese.arser.repository.AbstractRepository;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public final class HttpRepository extends AbstractRepository {
    public static HttpRepositoryBuilder builder() {
        return new HttpRepositoryBuilder();
    }

    private final HttpClient httpClient;

    HttpRepository(final URI uri, final String name, final HttpClient httpClient) {
        super(uri, name);

        this.httpClient = Objects.requireNonNull(httpClient, "httpClient required");
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .GET()
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .header(ArserUtils.HTTP_HEADER_ACCEPT, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                .build();

        try {
            final HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() == ArserUtils.HTTP_STATUS_OK) {
                try (InputStream inputStream = httpResponse.body()) {
                    return new ArserResult.Download(DefaultBlobValue.of(inputStream));
                }
            }

            try (InputStream inputStream = httpResponse.body()) {
                // Drain the Body.
                inputStream.transferTo(OutputStream.nullOutputStream());
            }

            return new ArserResult.NotFound(remoteUri);
        }
        catch (final InterruptedException ex) {
            // Preserve interrupt status.
            Thread.currentThread().interrupt();

            return new ArserResult.Failure(ex);
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(remoteUri)
                .HEAD()
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .build();

        try {
            final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());

            if (httpResponse.statusCode() == ArserUtils.HTTP_STATUS_OK) {
                return new ArserResult.Exist(remoteUri);
            }

            return new ArserResult.NotFound(remoteUri);
        }
        catch (final InterruptedException ex) {
            // Preserve interrupt status.
            Thread.currentThread().interrupt();

            return new ArserResult.Failure(ex);
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }

    private URI toRemoteUri(final URI uri, final ArserRequest arserRequest) {
        String pathResource = arserRequest.getResource().getPath();

        if (pathResource.startsWith("/")) {
            pathResource = pathResource.substring(1);
        }

        String newPath = uri.getPath();

        if (newPath.endsWith("/")) {
            newPath += pathResource;
        } else {
            newPath += "/" + pathResource;
        }

        return uri.resolve(newPath);

        // return new URI(
        //         baseUri.getScheme(),
        //         baseUri.getUserInfo(),
        //         baseUri.getHost(),
        //         baseUri.getPort(),
        //         newPath,
        //         baseUri.getQuery(),
        //         baseUri.getFragment()
        // );
    }
}
