package de.freese.arser.connector.http;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.security.CredentialsProvider;
import de.freese.arser.connector.security.UriGuard;
import de.freese.arser.connector.spi.AbstractConnector;
import de.freese.arser.connector.spi.ConnectorException;
import de.freese.arser.connector.spi.UnsupportedOperationForSchemeException;

/**
 * @author Thomas Freese
 */
public abstract class AbstractHttpConnector extends AbstractConnector {
    private final CredentialsProvider credentialsProvider;
    private final UriGuard uriGuard;

    protected AbstractHttpConnector(final UriGuard uriGuard, final CredentialsProvider credentialsProvider) {
        super(Set.of("http", "https"), Set.of(
                Operations.DELETE,
                Operations.DOWNLOAD,
                Operations.EXISTS,
                Operations.UPLOAD,
                Operations.UPLOAD_STREAM
        ));

        this.uriGuard = Objects.requireNonNull(uriGuard, "uriGuard required");
        this.credentialsProvider = Objects.requireNonNull(credentialsProvider, "credentialsProvider required");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        checkGuard(request.uri());

        final ConnectorResponse<?> response;

        if (Operations.DELETE.equals(request.operation())) {
            response = doDelete(request);
        } else if (Operations.DOWNLOAD.equals(request.operation())) {
            response = doDownload(request);
        } else if (Operations.EXISTS.equals(request.operation())) {
            response = doExists(request);
        } else if (Operations.HEAD.equals(request.operation())) {
            response = doHead(request);
        } else if (Operations.UPLOAD.equals(request.operation())) {
            response = doUpload(request);
        } else if (Operations.UPLOAD_STREAM.equals(request.operation())) {
            response = doUploadStream(request);
        } else {
            throw new UnsupportedOperationForSchemeException(request.operation().name(), request.uri().getScheme());
        }

        return (ConnectorResponse<R>) response;
    }

    protected void checkGuard(final URI uri) {
        if (!uriGuard.test(uri)) {
            throw new ConnectorException("Blocked by UriGuard: " + uri);
        }
    }

    protected abstract ConnectorResponse<Void> doDelete(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<BlobValue> doDownload(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<Boolean> doExists(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<Map<String, List<String>>> doHead(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<Void> doUpload(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<Long> doUploadStream(ConnectorRequest<?> request);

    protected CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }
}
