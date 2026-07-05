package de.freese.arser.connector.spi;

import java.io.Serial;

/**
 * @author Thomas Freese
 */
public class BlockedException extends ConnectorException {
    @Serial
    private static final long serialVersionUID = -1L;

    public BlockedException(final String message) {
        super(message);
    }
}
