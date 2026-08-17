package de.freese.arser.api;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.file.FileRepository;
import de.freese.arser.repository.http.HttpRepository;
import de.freese.arser.repository.virtual.VirtualRepository;

/**
 * @author Thomas Freese
 * @since 02.07.26
 */
public interface Arser {
    static Arser from(final ArserConfig config, final LifeCycleRegistry lifeCycleRegistry) {
        Objects.requireNonNull(config, "config required");
        Objects.requireNonNull(lifeCycleRegistry, "lifeCycleRegistry required");

        final Map<String, Repository> repositoryMap = new HashMap<>();

        config.fileRepositoryConfigs().forEach(fileRepoConfig -> {
            final Repository repository = FileRepository.of(fileRepoConfig, lifeCycleRegistry);
            repositoryMap.put(repository.getName(), repository);
        });

        config.httpRepositoryConfigs().forEach(httpRepoConfig -> {
            final Repository repository = HttpRepository.of(httpRepoConfig, lifeCycleRegistry);
            repositoryMap.put(repository.getName(), repository);
        });

        config.virtualRepositoryConfigs().forEach(virtualRepoConfig -> {
            final Repository repository = VirtualRepository.of(virtualRepoConfig, repositoryMap::get);
            repositoryMap.put(repository.getName(), repository);
        });

        return new DefaultArser(config, repositoryMap);
    }

    ArserResult download(String repositoryName, ArserRequest arserRequest);

    ArserResult exist(String repositoryName, ArserRequest arserRequest);

    ArserConfig getConfig();

    Map<String, Repository> getRepositories();

    ArserResult upload(String repositoryName, ArserRequest arserRequest, InputStream inputStream);
}
