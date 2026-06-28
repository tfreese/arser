package de.freese.arser.repository;

import java.io.Serial;

/**
 * @author Thomas Freese
 */
public class RepositoryException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -1L;

    public RepositoryException(final String message) {
        super(message);
    }

    public RepositoryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
