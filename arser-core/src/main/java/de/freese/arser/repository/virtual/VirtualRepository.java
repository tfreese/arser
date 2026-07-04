package de.freese.arser.repository.virtual;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualRepository.class);

    public static VirtualRepositoryBuilder builder() {
        return new VirtualRepositoryBuilder();
    }

    private final Map<String, Repository> repositoryMap;

    VirtualRepository(final URI uri, final String name, final Map<String, Repository> repositoryMap) {
        super(uri, name);

        this.repositoryMap = Objects.requireNonNull(repositoryMap, "repositoryMap required");
    }

    @Override
    public <R> ArserResult<R> download(final ArserRequest arserRequest) {
        BlobValue blobValue = null;

        for (final Repository repository : repositoryMap.values()) {
            final ArserResult<R> arserResult = repository.download(arserRequest);

            if (arserResult instanceof ArserResult.Download<R>(final BlobValue value)) {
                blobValue = value;

                getLogger().debug("{} was downloaded from {}", arserRequest.getResource(), repository.getName());

                break;
            } else if (arserResult instanceof ArserResult.Failure<R>(final Throwable cause)) {
                LOGGER.warn("{}: {} - {}", repository.getName(), cause.getClass().getSimpleName(), cause.getMessage());
            }
        }

        if (blobValue == null) {
            return new ArserResult.Exist<>(arserRequest.getResource(), false);
        }

        return new ArserResult.Download<>(blobValue);
    }

    @Override
    public <R> ArserResult<R> exist(final ArserRequest arserRequest) {
        boolean exist = false;

        for (final Repository repository : repositoryMap.values()) {
            final ArserResult<R> arserResult = repository.exist(arserRequest);

            if (arserResult instanceof ArserResult.Exist<R>) {
                exist = true;

                getLogger().debug("{} exist in {}", arserRequest.getResource(), repository.getName());

                break;
            } else if (arserResult instanceof ArserResult.Failure<R>(final Throwable cause)) {
                LOGGER.warn("{}: {} - {}", repository.getName(), cause.getClass().getSimpleName(), cause.getMessage());
            }
        }

        return new ArserResult.Exist<>(arserRequest.getResource(), exist);
    }
}
