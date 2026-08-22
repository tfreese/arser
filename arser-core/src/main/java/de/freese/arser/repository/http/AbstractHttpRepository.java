package de.freese.arser.repository.http;

import java.net.URI;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.repository.AbstractRepository;
import de.freese.arser.repository.AbstractRepositoryConfig;

/**
 * @author Thomas Freese
 * @since 22.08.26
 */
public abstract class AbstractHttpRepository extends AbstractRepository {
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
