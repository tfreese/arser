package de.freese.arser.core.connector;

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
}
