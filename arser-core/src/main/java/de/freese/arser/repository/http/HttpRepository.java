package de.freese.arser.repository.http;

import java.net.URI;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.repository.AbstractConnectedRepository;

/**
 * @author Thomas Freese
 */
public final class HttpRepository extends AbstractConnectedRepository {
    public static HttpRepositoryBuilder builder() {
        return new HttpRepositoryBuilder();
    }

    HttpRepository(final URI uri, final String name, final Connector connector) {
        super(uri, name, connector);
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
