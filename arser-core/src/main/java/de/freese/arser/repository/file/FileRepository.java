package de.freese.arser.repository.file;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.FileBlobValue;
import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.repository.AbstractRepository;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.decorator.LoggingRepositoryDecorator;

/**
 * @author Thomas Freese
 */
public final class FileRepository extends AbstractRepository {
    public static Repository of(final FileRepositoryConfig config, final LifeCycleRegistry lifeCycleRegistry) {
        Repository repository = new FileRepository(config);

        if (config.logging()) {
            repository = new LoggingRepositoryDecorator(repository);
        }

        lifeCycleRegistry.register(repository);

        return repository;
    }

    private FileRepository(final FileRepositoryConfig config) {
        super(config);
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final Path path = toAbsolutePath(getUri(), arserRequest);

        if (!Files.exists(path)) {
            return new ArserResult.NotFound(path.toUri());
        }

        return new ArserResult.Download(new FileBlobValue(path));
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final Path path = toAbsolutePath(getUri(), arserRequest);

        if (!Files.exists(path)) {
            return new ArserResult.NotFound(path.toUri());
        }

        return new ArserResult.Exist(path.toUri());
    }

    @Override
    public FileRepositoryConfig getConfig() {
        return (FileRepositoryConfig) super.getConfig();
    }

    @Override
    public void start() throws Exception {
        super.start();

        final Path path = Path.of(getUri());

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalStateException("path not readable: " + path);
        }
    }

    @Override
    public ArserResult upload(final ArserRequest arserRequest, final InputStream inputStream) {
        final Path path = toAbsolutePath(getUri(), arserRequest);

        if (isReadOnly()) {
            final String message = "repository is read only: %s [%s]".formatted(getName(), getClass().getSimpleName());

            return new ArserResult.Forbidden(path.toUri(), message);
        }

        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            }

            return new ArserResult.Upload(Files.size(path));
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }

    // @JsonProperty("readOnly")
    private boolean isReadOnly() {
        return getConfig().readOnly();
    }

    private Path toAbsolutePath(final URI uri, final ArserRequest arserRequest) {
        final String uriPath = uri.getPath();

        final String requestPath = arserRequest.getResource().getPath();

        return Path.of(uriPath).resolve(requestPath);
    }
}
