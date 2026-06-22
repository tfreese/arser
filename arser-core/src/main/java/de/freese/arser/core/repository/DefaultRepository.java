package de.freese.arser.core.repository;

import java.net.URI;

import de.freese.arser.core.model.ArserRequest;
import de.freese.arser.core.model.ArserResult;

/**
 * @author Thomas Freese
 */
public class DefaultRepository extends AbstractRepository {
    protected DefaultRepository(final URI uri) {
        super(uri);
    }

    @Override
    public boolean exist(final ArserRequest arserRequest) throws Exception {
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public <R> ArserResult<R> getResource(final ArserRequest arserRequest) throws Exception {
        return null;
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }
}
