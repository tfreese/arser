// Created: 22.07.23
package de.freese.arser.core.repository.remote;

import java.net.URI;

import de.freese.arser.core.repository.AbstractRepository;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRemoteRepository extends AbstractRepository {
    protected AbstractRemoteRepository(final String name, final URI uri) {
        super(name, uri);
    }

    protected URI createResourceUri(final URI uri, final URI resource) {
        String path = uri.getPath();
        final String pathResource = resource.getPath();

        if (path.endsWith("/") && pathResource.startsWith("/")) {
            path += pathResource.substring(1);
        }
        else if (path.endsWith("/") && !pathResource.startsWith("/")) {
            path += pathResource;
        }
        else if (!path.endsWith("/") && pathResource.startsWith("/")) {
            path += pathResource;
        }

        return uri.resolve(path);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final String scheme = getUri().getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            final String msg = "HTTP or HTTPS protocol required: " + scheme;

            getLogger().error(msg);

            throw new IllegalArgumentException(msg);
        }
    }
}
