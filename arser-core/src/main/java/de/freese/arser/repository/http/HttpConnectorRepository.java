package de.freese.arser.repository.http;

import java.net.URI;
import java.net.http.HttpClient;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.connector.http.JreHttpClientConnector;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.repository.AbstractConnectedRepository;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.decorator.CachingFileRepositoryDecorator;
import de.freese.arser.repository.decorator.LoggingRepositoryDecorator;
import de.freese.arser.repository.decorator.RetryingRepositoryDecorator;

/**
 * @author Thomas Freese
 */
public final class HttpConnectorRepository extends AbstractConnectedRepository {
    public static Repository of(final HttpRepositoryConfig config, final LifeCycleRegistry lifeCycleRegistry) {
        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(config.connectTimeout())
                // .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.ALWAYS);
        final HttpClient httpClient = httpClientBuilder.build();
        lifeCycleRegistry.register(httpClient);

        Repository repository = new HttpConnectorRepository(config, new JreHttpClientConnector(httpClient));

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

    private HttpConnectorRepository(final HttpRepositoryConfig config, final Connector connector) {
        super(config, connector);
    }

    @Override
    public HttpRepositoryConfig getConfig() {
        return (HttpRepositoryConfig) super.getConfig();
    }
    
    @Override
    protected URI toRemoteUri(final URI uri, final ArserRequest arserRequest) {
        String pathResource = arserRequest.getResource().getPath();

        if (pathResource.startsWith("/")) {
            pathResource = pathResource.substring(1);
        }

        String newPath = uri.getPath();

        if (newPath.endsWith("/")) {
            newPath += pathResource;
        }
        else {
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
