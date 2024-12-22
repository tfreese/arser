// Created: 22.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.AbstractResponseHandlerAdapter;
import de.freese.arser.core.response.ResponseHandler;

/**
 * @author Thomas Freese
 */
public class VirtualRepository extends AbstractRepository {

    private static final class VirtualResponseHandler extends AbstractResponseHandlerAdapter {
        private final ResponseHandler responseHandler;

        private boolean success;

        private VirtualResponseHandler(final ResponseHandler responseHandler) {
            super();

            this.responseHandler = Objects.requireNonNull(responseHandler, "responseHandler required");
        }

        @Override
        public void onError(final Exception exception) {
            responseHandler.onError(exception);
        }

        @Override
        public void onSuccess(final long contentLength, final InputStream inputStream) {
            responseHandler.onSuccess(contentLength, inputStream);

            success = true;
        }

        private boolean isSuccess() {
            return success;
        }
    }

    private final Map<String, Repository> repositoryMap = new HashMap<>();

    public VirtualRepository(final String contextRoot, final List<Repository> repositories) {
        super(contextRoot, URI.create("virtual"));

        for (Repository repository : repositories) {
            if (repositoryMap.containsKey(repository.getContextRoot())) {
                throw new IllegalStateException("repository already exist: " + repository.getContextRoot());
            }

            repositoryMap.put(repository.getContextRoot(), repository);
        }
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        boolean exist = false;

        for (final Repository repository : repositoryMap.values()) {
            try {
                exist = repository.exist(request);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (exist) {
                break;
            }
        }

        return exist;
    }

    @Override
    protected void doStart() throws Exception {
        // Empty
    }

    @Override
    protected void doStop() throws Exception {
        // Empty
    }

    @Override
    protected void doStreamTo(final ResourceRequest resourceRequest, final ResponseHandler handler) throws Exception {
        final VirtualResponseHandler virtualResponseHandler = new VirtualResponseHandler(handler);

        for (final Repository repository : repositoryMap.values()) {
            try {
                repository.streamTo(resourceRequest, virtualResponseHandler);
            }
            catch (final Exception ex) {
                getLogger().warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            }

            if (virtualResponseHandler.isSuccess()) {
                break;
            }
        }
    }
}
