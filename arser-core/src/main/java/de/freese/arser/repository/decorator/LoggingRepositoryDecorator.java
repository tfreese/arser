package de.freese.arser.repository.decorator;

import java.io.InputStream;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 */
public final class LoggingRepositoryDecorator extends AbstractRepositoryDecorator {
    public LoggingRepositoryDecorator(final Repository delegate) {
        super(delegate);
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        getLogger().debug("download from '{}': {}", delegate().getName(), arserRequest);

        return super.download(arserRequest);
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        getLogger().debug("exist in '{}': {}", delegate().getName(), arserRequest);

        return super.exist(arserRequest);
    }

    @Override
    public void start() throws Exception {
        getLogger().debug("starting repository '{}': {}", delegate().getName(), getName());

        super.start();
    }

    @Override
    public void stop() throws Exception {
        getLogger().debug("stopping repository '{}': {}", delegate().getName(), getName());

        super.stop();
    }

    @Override
    public ArserResult upload(final ArserRequest arserRequest, final InputStream inputStream) {
        getLogger().debug("upload to '{}': {}", delegate().getName(), arserRequest);

        return super.upload(arserRequest, inputStream);
    }
}
