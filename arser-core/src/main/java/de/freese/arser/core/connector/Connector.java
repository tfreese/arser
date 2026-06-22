package de.freese.arser.core.connector;

import java.io.InputStream;

import de.freese.arser.core.component.Lifecycle;
import de.freese.arser.core.model.ArserRequest;
import de.freese.arser.core.model.BlobValue;

/**
 * For HTTP, File and JDBC.
 *
 * @author Thomas Freese
 */
public interface Connector extends Lifecycle {
    boolean exist(ArserRequest arserRequest) throws Exception;

    BlobValue getResource(ArserRequest arserRequest) throws Exception;

    default void write(final ArserRequest arserRequest, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("connector is read only: " + getClass().getSimpleName());
    }
}
