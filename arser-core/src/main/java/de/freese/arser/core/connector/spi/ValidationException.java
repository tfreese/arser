package de.freese.arser.core.connector.spi;

import java.io.Serial;

/**
 * @author Thomas Freese
 */
public class ValidationException extends ConnectorException {
    @Serial
    private static final long serialVersionUID = -1L;

    public ValidationException(final String message) {
        super(message);
    }
}
