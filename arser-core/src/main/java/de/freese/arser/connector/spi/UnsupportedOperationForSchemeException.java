package de.freese.arser.connector.spi;

import java.io.Serial;

/**
 * @author Thomas Freese
 */
public class UnsupportedOperationForSchemeException extends ConnectorException {
    @Serial
    private static final long serialVersionUID = -1L;

    public UnsupportedOperationForSchemeException(final String operation, final String scheme) {
        super("Operation '" + operation + "' not supported for Scheme '" + scheme + "'");
    }
}
