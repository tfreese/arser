// Created: 22.07.23
package de.freese.arser.core.api;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepository extends AbstractLifecycle implements Repository {

    private final String contextRoot;
    private final URI uri;

    protected AbstractRepository(final String contextRoot, final URI uri) {
        super();

        this.contextRoot = Objects.requireNonNull(contextRoot, "contextRoot required");
        this.uri = Objects.requireNonNull(uri, "uri required");
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
    public String getContextRoot() {
        return contextRoot;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public void streamTo(final ResourceRequest resourceRequest, final ResponseHandler handler) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource: {}", resourceRequest.getResource());
        }

        doStreamTo(resourceRequest, handler);
    }

    @Override
    public String toString() {
        return getContextRoot() + ": " + getUri();
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

    protected abstract void doStreamTo(ResourceRequest resourceRequest, ResponseHandler handler) throws Exception;

    protected void doWrite(final ResourceRequest request, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getUri());
    }
}
