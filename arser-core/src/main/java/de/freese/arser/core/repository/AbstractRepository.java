// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepository extends AbstractLifecycle implements Repository {

    private final String name;
    private final URI uri;

    protected AbstractRepository(final String name, final URI uri) {
        super();

        if (assertNotNull(name, () -> "Name").isBlank()) {
            throw new IllegalArgumentException("name is empty");
        }

        this.name = name;
        this.uri = assertNotNull(uri, () -> "URI");
    }

    @Override
    public boolean exist(final ResourceRequest request) throws Exception {
        if (!isStarted()) {
            getLogger().warn("Component not started: {}", getName());
            return false;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist: {}", request.getResource());
        }

        return doExist(request);
    }

    @Override
    public ResourceResponse getInputStream(final ResourceRequest request) throws Exception {
        if (!isStarted()) {
            getLogger().warn("Component not started: {}", getName());
            return null;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream: {}", request.getResource());
        }

        return doGetInputStream(request);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return getName() + ": " + getUri();
    }

    @Override
    public void write(final ResourceRequest request, final InputStream inputStream) throws Exception {
        if (!isStarted()) {
            getLogger().warn("Component not started: {}", getName());
            return;
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("write: {}", request.getResource());
        }

        doWrite(request, inputStream);
    }

    protected abstract boolean doExist(ResourceRequest request) throws Exception;

    protected abstract ResourceResponse doGetInputStream(ResourceRequest request) throws Exception;

    protected void doWrite(final ResourceRequest request, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getName() + " - " + getUri());
    }
}
