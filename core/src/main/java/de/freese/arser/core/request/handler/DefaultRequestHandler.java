// Created: 17.01.24
package de.freese.arser.core.request.handler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.RepositoryResponse;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.request.ResourceResponse;

/**
 * @author Thomas Freese
 */
public class DefaultRequestHandler implements RequestHandler {
    private final Map<String, Repository> repositoryMap = new HashMap<>();

    @Override
    public void addRepository(final String contextRoot, final Repository repository) {
        if (repositoryMap.containsKey(contextRoot)) {
            throw new IllegalStateException("contextRoot already exist: " + contextRoot);
        }

        repositoryMap.put(contextRoot, repository);
    }

    @Override
    public boolean exist(final ResourceRequest resourceRequest) throws Exception {
        final Repository repository = getRepository(resourceRequest.getContextRoot());

        return repository.exist(resourceRequest.getResource());
    }

    @Override
    public ResourceResponse getInputStream(final ResourceRequest resourceRequest) throws Exception {
        final Repository repository = getRepository(resourceRequest.getContextRoot());

        final RepositoryResponse repositoryResponse = repository.getInputStream(resourceRequest.getResource());

        return new ResourceResponse(resourceRequest, repositoryResponse.getContentLength(), repositoryResponse);
    }

    @Override
    public void write(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        final Repository repository = getRepository(resourceRequest.getContextRoot());

        if (!repository.isWriteable()) {
            throw new IllegalStateException("Repository is not writeable: " + repository.getName());
        }

        repository.write(resourceRequest.getResource(), inputStream);
    }

    protected Repository getRepository(final String contextRoot) {
        final Repository repository = repositoryMap.get(contextRoot);

        if (repository == null) {
            throw new IllegalArgumentException("No Repository not found for contextRoot: " + contextRoot);
        }

        return repository;
    }
}
