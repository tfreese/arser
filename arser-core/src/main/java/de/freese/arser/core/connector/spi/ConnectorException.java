package de.freese.arser.core.connector.spi;

import java.io.Serial;

/**
 * @author Thomas Freese
 */
public class ConnectorException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -1L;

    public ConnectorException(final String message) {
        super(message);
    }

    public ConnectorException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
