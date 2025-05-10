// Created: 21 Dez. 2024
package de.freese.arser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import de.freese.arser.config.ConfigValidator;
import de.freese.arser.core.repository.Repository;

/**
 * ARtifact-SERvice<br>
 * Inspired by <a href="https://github.com/sonatype/nexus-public">nexus-public</a>
 *
 * @author Thomas Freese
 */
public final class Arser {
    private final Map<String, Repository> repositoryMap = new HashMap<>();

    public Arser() {
        this(Path.of(System.getProperty("java.io.tmpdir"), "arser"));
    }

    public Arser(final Path workingDir) {
        super();

        try {
            Files.createDirectories(workingDir);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        
        System.setProperty("arser.workingDir", workingDir.toAbsolutePath().toString());
    }

    public void addRepository(final Repository repository) {
        Objects.requireNonNull(repository, "repository required");

        final String contextRoot = repository.getContextRoot();

        ConfigValidator.contextRoot(contextRoot);

        if (repositoryMap.containsKey(contextRoot)) {
            throw new IllegalStateException("repository already exist for contextRoot: " + contextRoot);
        }

        repositoryMap.put(contextRoot, repository);
    }

    public void forEach(final Consumer<Repository> consumer) {
        repositoryMap.values().forEach(consumer);
    }

    public Repository getRepository(final String contextRoot) {
        return repositoryMap.get(contextRoot);
    }

    public int getRepositoryCount() {
        return repositoryMap.size();
    }

    public Path getWorkingDir() {
        return Path.of(System.getProperty("arser.workingDir"));
    }
}
