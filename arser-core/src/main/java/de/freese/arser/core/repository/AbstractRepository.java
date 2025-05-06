// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;

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
    public final boolean exist(final ResourceRequest request) throws Exception {
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
    public final URI getDownloadUri(final ResourceRequest request) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getDownloadUri: {}", request.getResource());
        }

        return doGetDownloadUri(request);
    }

    @Override
    public final ResourceResponse getResource(final ResourceRequest request) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getResource: {}", request.getResource());
        }

        return doGetResource(request);
    }

    @Override
    public String toString() {
        return getContextRoot() + ": " + getBaseUri();
    }

    @Override
    public final void write(final ResourceRequest request, final InputStream inputStream) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("write: {}", request.getResource());
        }

        doWrite(request, inputStream);
    }

    protected abstract boolean doExist(ResourceRequest request) throws Exception;

    protected abstract URI doGetDownloadUri(ResourceRequest request) throws Exception;

    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        // TODO
        return null;
    }

    protected void doWrite(final ResourceRequest request, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getBaseUri());
    }
}
