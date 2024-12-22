// Created: 21 Dez. 2024
package de.freese.arser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.freese.arser.core.repository.Repository;

/**
 * ARtifact-SERvice<br>
 * Inspired by <a href="https://github.com/sonatype/nexus-public">nexus-public</a>
 *
 * @author Thomas Freese
 */
public final class Arser {
    private static final Pattern PATTERN_CONTEXT_ROOT = Pattern.compile("([a-z0-9\\-_])+");

    public static void validateContextRoot(final String contextRoot) {
        if (!PATTERN_CONTEXT_ROOT.matcher(contextRoot).matches()) {
            throw new IllegalArgumentException("contextRoot must match the pattern: " + PATTERN_CONTEXT_ROOT);
        }
    }

    private final Map<String, Repository> repositoryMap = new HashMap<>();

    public void addRepository(final Repository repository) {
        Objects.requireNonNull(repository, "repository required");

        final String contextRoot = repository.getContextRoot();

        validateContextRoot(contextRoot);

        if (repositoryMap.containsKey(contextRoot)) {
            throw new IllegalStateException("repository already exist for contextRoot: " + contextRoot);
        }

        repositoryMap.put(contextRoot, repository);
    }

    public Repository getRepository(final String contextRoot) {
        return repositoryMap.get(contextRoot);
    }

    public int getRepositoryCount() {
        return repositoryMap.size();
    }
}
