package de.freese.arser.repository.http;

import java.net.URI;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.repository.AbstractRepository;
import de.freese.arser.repository.AbstractRepositoryConfig;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.decorator.CachingFileRepositoryDecorator;
import de.freese.arser.repository.decorator.LoggingRepositoryDecorator;
import de.freese.arser.repository.decorator.RetryingRepositoryDecorator;

/**
 * @author Thomas Freese
 * @since 22.08.26
 */
public abstract class AbstractHttpRepository extends AbstractRepository {
    protected static Repository configure(final Repository repository, final HttpRepositoryConfig config) {
        Repository repositoryDecorated = repository;

        if (config.maxRetries() > 0) {
            repositoryDecorated = new RetryingRepositoryDecorator(repositoryDecorated, config.maxRetries(), config.retryInterval());
        }

        if (config.cachingPath() != null) {
            repositoryDecorated = new CachingFileRepositoryDecorator(repositoryDecorated, config.cachingPath());
        }

        if (config.logging()) {
            repositoryDecorated = new LoggingRepositoryDecorator(repositoryDecorated);
        }

        return repositoryDecorated;
    }

    protected AbstractHttpRepository(final AbstractRepositoryConfig config) {
        super(config);
    }

    @Override
    public HttpRepositoryConfig getConfig() {
        return (HttpRepositoryConfig) super.getConfig();
    }

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
