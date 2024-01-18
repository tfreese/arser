// Created: 17.01.24
package de.freese.arser.core.request.handler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.request.ResourceResponse;

/**
 * @author Thomas Freese
 */
public class DefaultRequestHandler implements RequestHandler {
    private final Map<String, Repository> repositoryMap = new HashMap<>();

    @Override
    public void addRepository(final Repository repository) {
        final String name = repository.getName();

        if (repositoryMap.containsKey(name)) {
            throw new IllegalStateException("repository already exist: " + name);
        }

        repositoryMap.put(name, repository);
    }

    @Override
    public boolean exist(final ResourceRequest resourceRequest) throws Exception {
        final Repository repository = getRepository(resourceRequest.getContextRoot());

        return repository.exist(resourceRequest);
    }

    @Override
    public ResourceResponse getInputStream(final ResourceRequest resourceRequest) throws Exception {
        final Repository repository = getRepository(resourceRequest.getContextRoot());

        return repository.getInputStream(resourceRequest);
    }

    @Override
    public void write(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        final Repository repository = getRepository(resourceRequest.getContextRoot());

        if (!repository.isWriteable()) {
            throw new IllegalStateException("Repository is not writeable: " + repository.getName());
        }

        repository.write(resourceRequest, inputStream);
    }

    protected Repository getRepository(final String name) {
        final Repository repository = repositoryMap.get(name);

        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + name);
        }

        return repository;
    }
}
