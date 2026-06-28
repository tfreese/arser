// Created: 22.07.23
package de.freese.arser.repository.virtual;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.model.ArserRequest;
import de.freese.arser.model.ArserResult;
import de.freese.arser.repository.AbstractRepository;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.RepositoryException;

/**
 * @author Thomas Freese
 */
public final class VirtualRepository extends AbstractRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualRepository.class);

    private final Map<String, Repository> repositoryMap = new LinkedHashMap<>();

    public VirtualRepository(final String name) {
        super(URI.create("virtual-" + name), name);
    }

    public VirtualRepository add(final Repository repository) {
        if (repositoryMap.containsKey(repository.getName())) {
            throw new RepositoryException("repository already exist: " + repository.getName());
        }

        repositoryMap.put(repository.getName(), repository);

        return this;
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
            return new ArserResult.NotFound<>(arserRequest.getResource());
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

        return new ArserResult.Exist<>(exist);
    }

}
