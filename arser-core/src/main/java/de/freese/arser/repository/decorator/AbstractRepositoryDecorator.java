package de.freese.arser.repository.decorator;

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

    protected AbstractRepositoryDecorator(final Repository delegate) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
    }

    @Override
    public <R> ArserResult<R> download(final ArserRequest arserRequest) {
        return getDelegate().download(arserRequest);
    }

    @Override
    public <R> ArserResult<R> exist(final ArserRequest arserRequest) {
        return delegate.exist(arserRequest);
    }

    @Override
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    public URI getUri() {
        return getDelegate().getUri();
    }

    protected Repository getDelegate() {
        return delegate;
    }

    protected Logger getLogger() {
        if (getDelegate() instanceof final AbstractRepositoryDecorator ard) {
            return ard.getLogger();
        }

        return LoggerFactory.getLogger(getDelegate().getClass());
    }
}
