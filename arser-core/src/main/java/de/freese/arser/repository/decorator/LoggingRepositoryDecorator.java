package de.freese.arser.repository.decorator;

import java.io.InputStream;

import de.freese.arser.model.ArserRequest;
import de.freese.arser.model.ArserResult;
import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 */
public final class LoggingRepositoryDecorator extends AbstractRepositoryDecorator {
    public LoggingRepositoryDecorator(final Repository delegate) {
        super(delegate);
    }

    @Override
    public <R> ArserResult<R> download(final ArserRequest arserRequest) {
        getLogger().debug("download: {}", arserRequest);

        return super.download(arserRequest);
    }

    @Override
    public <R> ArserResult<R> exist(final ArserRequest arserRequest) {
        getLogger().debug("exist: {}", arserRequest);

        return super.exist(arserRequest);
    }

    @Override
    public void start() throws Exception {
        getLogger().debug("starting repository: {}", getName());

        super.start();
    }

    @Override
    public void stop() throws Exception {
        getLogger().debug("stopping repository: {}", getName());

        super.stop();
    }

    @Override
    public <R> ArserResult<R> upload(final ArserRequest arserRequest, final InputStream inputStream) {
        getLogger().debug("upload: {}", arserRequest);

        return super.upload(arserRequest, inputStream);
    }
}
