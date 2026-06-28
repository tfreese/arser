package de.freese.arser.repository.http;

import java.net.URI;

import de.freese.arser.connector.spi.Connector;
import de.freese.arser.repository.AbstractConnectedRepository;

/**
 * @author Thomas Freese
 */
public final class HttpRepository extends AbstractConnectedRepository {

    public HttpRepository(final URI uri, final String name, final Connector connector) {
        super(uri, name, connector);
    }
}
