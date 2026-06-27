package de.freese.arser.connector;

import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.core.component.Lifecycle;
import de.freese.arser.core.model.ArserRequest;

/**
 * For HTTP, File and JDBC.
 *
 * @author Thomas Freese
 */
public interface Connector extends Lifecycle {
    boolean exist(ArserRequest arserRequest) throws Exception;

    BlobValue getResource(ArserRequest arserRequest) throws Exception;
}
