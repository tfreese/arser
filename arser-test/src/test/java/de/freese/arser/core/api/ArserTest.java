// Created: 19 Dez. 2024
package de.freese.arser.core.api;

import java.util.HashMap;
import java.util.Map;

import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public final class ArserTest {
    private final Map<String, RepositoryTest> map = new HashMap<>();

    public RepositoryTest getRepository(final ResourceRequest resourceRequest) {
        final RepositoryTest repository = map.get(resourceRequest.getContextRoot());

        if (repository.contains(resourceRequest)) {
            return repository;
        }

        return null;
    }
}
