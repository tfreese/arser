package de.freese.arser.core.repository;

import java.io.InputStream;

import de.freese.arser.core.model.ArserRequest;

/**
 * @author Thomas Freese
 */
public interface WriteableRepository extends Repository {
    void write(ArserRequest arserRequest, InputStream inputStream) throws Exception;
}
