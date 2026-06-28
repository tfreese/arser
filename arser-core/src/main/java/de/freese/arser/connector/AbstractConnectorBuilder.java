package de.freese.arser.connector;

import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
public abstract class AbstractConnectorBuilder<B> {

    public abstract Connector build() throws Exception;

    protected abstract B self();
}
