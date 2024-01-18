// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.request.ResourceResponse;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepository extends AbstractLifecycle implements Repository {

    private final String name;
    private final URI uri;

    protected AbstractRepository(final String name, final URI uri) {
        super();

        if (name.isBlank()) {
            throw new IllegalArgumentException("name is empty");
        }

        this.name = checkNotNull(name, "Name");
        this.uri = checkNotNull(uri, "URI");
    }

    @Override
    public boolean exist(final ResourceRequest resourceRequest) throws Exception {
        if (!isStarted()) {
            getLogger().warn("Component not started: {}", getName());
            return false;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist: {}", resourceRequest.getResource());
        }

        return doExist(resourceRequest);
    }

    @Override
    public ResourceResponse getInputStream(final ResourceRequest resourceRequest) throws Exception {
        if (!isStarted()) {
            getLogger().warn("Component not started: {}", getName());
            return null;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream: {}", resourceRequest.getResource());
        }

        return doGetInputStream(resourceRequest);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName() + ": " + getUri();
    }

    @Override
    public void write(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        if (!isStarted()) {
            getLogger().warn("Component not started: {}", getName());
            return;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("write: {}", resourceRequest.getResource());
        }

        doWrite(resourceRequest, inputStream);
    }

    protected abstract boolean doExist(ResourceRequest resourceRequest) throws Exception;

    protected abstract ResourceResponse doGetInputStream(ResourceRequest resourceRequest) throws Exception;

    protected void doWrite(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getName() + " - " + getUri());
    }

    protected URI getUri() {
        return uri;
    }
}
