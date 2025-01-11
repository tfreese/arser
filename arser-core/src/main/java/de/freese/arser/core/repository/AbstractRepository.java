// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResponseHandler;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepository extends AbstractLifecycle implements Repository {

    private final URI baseUri;
    private final String contextRoot;

    protected AbstractRepository(final String contextRoot, final URI baseUri) {
        super();

        this.contextRoot = Objects.requireNonNull(contextRoot, "contextRoot required");
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri required");
    }

    @Override
    public boolean exist(final ResourceRequest request) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist: {}", request.getResource());
        }

        return doExist(request);
    }

    @Override
    public URI getBaseUri() {
        return baseUri;
    }

    @Override
    public String getContextRoot() {
        return contextRoot;
    }

    @Override
    public void streamTo(final ResourceRequest request, final ResponseHandler handler) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource: {}", request.getResource());
        }

        doStreamTo(request, handler);
    }

    @Override
    public String toString() {
        return getContextRoot() + ": " + getBaseUri();
    }

    @Override
    public void write(final ResourceRequest request, final InputStream inputStream) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("write: {}", request.getResource());
        }

        doWrite(request, inputStream);
    }

    protected abstract boolean doExist(ResourceRequest request) throws Exception;

    protected abstract void doStreamTo(ResourceRequest request, ResponseHandler handler) throws Exception;

    protected void doWrite(final ResourceRequest request, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getBaseUri());
    }
}
