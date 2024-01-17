// Created: 22.07.23
package de.freese.arser.core.repository.virtual;

import java.net.URI;
import java.util.concurrent.CopyOnWriteArrayList;

import de.freese.arser.core.repository.AbstractRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.RepositoryResponse;

/**
 * @author Thomas Freese
 */
public class DefaultVirtualRepository extends AbstractRepository {

    private final CopyOnWriteArrayList<Repository> repositories = new CopyOnWriteArrayList<>();

    public DefaultVirtualRepository(final String name) {
        super(name, URI.create("virtual"));
    }

    public void add(final Repository repository) {
        checkNotNull(repository, "Repository");

        final boolean added = repositories.addIfAbsent(repository);

        if (added) {
            getLogger().trace("Added: {}", repository);
        }
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    protected boolean doExist(final URI resource) {
        boolean exist = false;

        for (final Repository repository : repositories) {
            try {
                exist = repository.exist(resource);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (exist) {
                break;
            }
        }

        return exist;
    }

    @Override
    protected RepositoryResponse doGetInputStream(final URI resource) {
        RepositoryResponse response = null;

        for (final Repository repository : repositories) {
            try {
                response = repository.getInputStream(resource);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (response != null) {
                break;
            }
        }

        return response;
    }
}
