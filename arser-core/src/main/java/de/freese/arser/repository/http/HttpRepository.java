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
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.decorator.CachingFileRepositoryDecorator;
import de.freese.arser.repository.decorator.LoggingRepositoryDecorator;
import de.freese.arser.repository.decorator.RetryingRepositoryDecorator;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public final class HttpRepository extends AbstractHttpRepository {
    public static Repository of(final HttpRepositoryConfig config, final LifeCycleRegistry lifeCycleRegistry) {
        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(config.connectTimeout())
                // .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.ALWAYS);

        // if (authenticator != null) {
        //     httpClientBuilder = httpClientBuilder.authenticator(authenticator);
        // }

        final HttpClient httpClient = httpClientBuilder.build();
        lifeCycleRegistry.register(httpClient);

        Repository repository = new HttpRepository(config, httpClient);

        if (config.maxRetries() > 0) {
            repository = new RetryingRepositoryDecorator(repository, config.maxRetries(), config.retryInterval());
        }

        if (config.cachingPath() != null) {
            repository = new CachingFileRepositoryDecorator(repository, config.cachingPath());
        }

        if (config.logging()) {
            repository = new LoggingRepositoryDecorator(repository);
        }

        lifeCycleRegistry.register(repository);

        return repository;
    }

    private final HttpClient httpClient;

    private HttpRepository(final HttpRepositoryConfig config, final HttpClient httpClient) {
        super(config);

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
}
