package de.freese.arser.repository.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.DefaultBlobValue;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.RepositoryException;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public final class HttpRepository extends AbstractHttpRepository {
    public static Repository of(final HttpRepositoryConfig config) {
        final Repository repository = new HttpRepository(config);

        return configure(repository, config);
    }

    private HttpClient httpClient;

    private HttpRepository(final HttpRepositoryConfig config) {
        super(config);
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

            throw new RepositoryException(ex);
        }
        catch (final Exception ex) {
            throw new RepositoryException(ex);
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

            throw new RepositoryException(ex);
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

        // if (authenticator != null) {
        //     httpClientBuilder = httpClientBuilder.authenticator(authenticator);
        // }

        httpClient = httpClientBuilder.build();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        // httpClient.close();
        httpClient.shutdownNow();
        httpClient = null;
    }
}
