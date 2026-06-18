package de.freese.arser.core.model;

/**
 * For HTTP, File and JDBC.
 *
 * @author Thomas Freese
 */
public interface Connector {
    boolean exist(ResourceRequest resourceRequest) throws Exception;

    BlobValue getResource(ResourceRequest resourceRequest) throws Exception;
}
