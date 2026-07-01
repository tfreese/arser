package de.freese.arser.repository.decorator;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.model.ArserRequest;
import de.freese.arser.model.ArserResult;
import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepositoryDecorator implements Repository {
    private final Repository delegate;
    private final Logger logger;

    protected AbstractRepositoryDecorator(final Repository delegate) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");

        if (delegate instanceof final AbstractRepositoryDecorator ard) {
            logger = ard.getLogger();
        } else {
            logger = LoggerFactory.getLogger(delegate.getClass());
        }
    }

    @Override
    public <R> ArserResult<R> download(final ArserRequest arserRequest) {
        return delegate.download(arserRequest);
    }

    @Override
    public <R> ArserResult<R> exist(final ArserRequest arserRequest) {
        return delegate.exist(arserRequest);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public URI getUri() {
        return delegate.getUri();
    }

    @Override
    public void start() throws Exception {
        delegate.start();
    }

    @Override
    public void stop() throws Exception {
        delegate.stop();
    }

    @Override
    public <R> ArserResult<R> upload(final ArserRequest arserRequest, final InputStream inputStream) {
        return delegate.upload(arserRequest, inputStream);
    }

    protected Logger getLogger() {
        return logger;
    }
}
