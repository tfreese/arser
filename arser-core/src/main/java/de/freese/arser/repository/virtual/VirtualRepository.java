package de.freese.arser.repository.virtual;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.repository.AbstractRepository;
import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 * @since 22.07.23
 */
public final class VirtualRepository extends AbstractRepository {
    public static Repository of(final VirtualRepositoryConfig config, final Function<String, Repository> repositoryProvider) {
        Objects.requireNonNull(repositoryProvider, "repositoryProvider required");

        if (config.repositoryRefs().isEmpty()) {
            throw new IllegalStateException("No repositories names are defined");
        }

        final List<Repository> repositories = new ArrayList<>();

        for (final String repositoryName : config.repositoryRefs()) {
            final Repository repository = repositoryProvider.apply(repositoryName);

            if (repository == null) {
                throw new IllegalStateException("Repository not found: " + repositoryName);
            }

            repositories.add(repository);
        }

        return new VirtualRepository(config, repositories);
    }

    private final List<Repository> repositories;

    private VirtualRepository(final VirtualRepositoryConfig config, final List<Repository> repositories) {
        super(config);

        this.repositories = Objects.requireNonNull(repositories, "repositories required");
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        BlobValue blobValue = null;

        for (final Repository repository : repositories) {
            final ArserResult arserResult = repository.download(arserRequest);

            if (arserResult instanceof ArserResult.Download(final BlobValue value)) {
                blobValue = value;

                getLogger().debug("{} was downloaded from '{}'", arserRequest.getResource(), repository.getName());

                break;
            }
            else if (arserResult instanceof ArserResult.Failure(final Throwable cause)) {
                getLogger().warn("{}: {} - {}", repository.getName(), cause.getClass().getSimpleName(), cause.getMessage());
            }
        }

        if (blobValue == null) {
            return new ArserResult.NotFound(arserRequest.getResource());
        }

        return new ArserResult.Download(blobValue);
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        boolean exist = false;

        for (final Repository repository : repositories) {
            final ArserResult arserResult = repository.exist(arserRequest);

            if (arserResult instanceof ArserResult.Exist) {
                exist = true;

                getLogger().debug("{} exist in '{}'", arserRequest.getResource(), repository.getName());

                break;
            }
            else if (arserResult instanceof ArserResult.Failure(final Throwable cause)) {
                getLogger().warn("{}: {} - {}", repository.getName(), cause.getClass().getSimpleName(), cause.getMessage());
            }
        }

        if (exist) {
            return new ArserResult.Exist(arserRequest.getResource());
        }

        return new ArserResult.NotFound(arserRequest.getResource());
    }

    @Override
    public VirtualRepositoryConfig getConfig() {
        return (VirtualRepositoryConfig) super.getConfig();
    }
}
