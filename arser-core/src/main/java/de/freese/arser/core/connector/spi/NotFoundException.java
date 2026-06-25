package de.freese.arser.core.connector.spi;

import java.io.Serial;

/**
 * @author Thomas Freese
 */
public class NotFoundException extends ConnectorException {
    @Serial
    private static final long serialVersionUID = -1L;

    public NotFoundException(final String message) {
        super(message);
    }
}
