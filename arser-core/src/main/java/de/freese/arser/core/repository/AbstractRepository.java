// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.model.FileResource;
import de.freese.arser.core.model.ResourceRequest;
import de.freese.arser.core.utils.ArserUtils;

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
    public final boolean exist(final ResourceRequest resourceRequest) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist: {}", resourceRequest.getResource());
        }

        return doExist(resourceRequest);
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
    public final FileResource getResource(final ResourceRequest resourceRequest) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getResource: {}", resourceRequest.getResource());
        }

        return doGetResource(resourceRequest);
    }

    @Override
    public String toString() {
        return getContextRoot() + ": " + getBaseUri();
    }

    @Override
    public final void write(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        if (!isStarted()) {
            throw new IllegalStateException("Component not started: " + getContextRoot());
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("write: {}", resourceRequest.getResource());
        }

        doWrite(resourceRequest, inputStream);
    }

    protected abstract boolean doExist(ResourceRequest resourceRequest) throws Exception;

    protected abstract FileResource doGetResource(ResourceRequest resourceRequest) throws Exception;

    protected void doWrite(final ResourceRequest resourceRequest, final InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("read only repository: " + getContextRoot() + " - " + getBaseUri());
    }

    protected Path writeToTempFile(final Path workingDir, final URI uri, final InputStream inputStream) throws IOException {
        final Path path = workingDir.resolve(System.nanoTime() + ArserUtils.toFileName(uri));

        try (OutputStream outputStream = Files.newOutputStream(path);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
            inputStream.transferTo(bufferedOutputStream);

            bufferedOutputStream.flush();
        }

        return path;
    }
}
