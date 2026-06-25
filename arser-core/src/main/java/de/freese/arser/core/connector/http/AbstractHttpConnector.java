package de.freese.arser.core.connector.http;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.core.connector.api.ConnectorRequest;
import de.freese.arser.core.connector.api.ConnectorResponse;
import de.freese.arser.core.connector.core.Operations;
import de.freese.arser.core.connector.security.CredentialsProvider;
import de.freese.arser.core.connector.security.UriGuard;
import de.freese.arser.core.connector.spi.Connector;
import de.freese.arser.core.connector.spi.ConnectorException;
import de.freese.arser.core.connector.spi.UnsupportedOperationForSchemeException;

/**
 * @author Thomas Freese
 */
public abstract class AbstractHttpConnector implements Connector {
    private final CredentialsProvider credentialsProvider;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UriGuard uriGuard;

    protected AbstractHttpConnector(final UriGuard uriGuard, final CredentialsProvider credentialsProvider) {
        super();

        this.uriGuard = Objects.requireNonNull(uriGuard, "uriGuard required");
        this.credentialsProvider = Objects.requireNonNull(credentialsProvider, "credentialsProvider required");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        checkGuard(request.uri());

        return (ConnectorResponse<R>) switch (request.operation().name()) {
            case "exists" -> doExists(request);
            case "head" -> doHead(request);
            case "download" -> doDownload(request);
            case "download.stream" -> doDownloadStream(request);
            case "upload" -> doUpload(request);
            case "upload.stream" -> doUploadStream(request);
            case "delete" -> doDelete(request);
            default -> throw new UnsupportedOperationForSchemeException(request.operation().name(), request.uri().getScheme());
        };
    }

    @Override
    public Set<String> supportedOperations() {
        return Set.of(Operations.EXISTS.name(),
                Operations.HEAD.name(),
                Operations.DOWNLOAD.name(),
                Operations.DOWNLOAD_STREAM.name(),
                Operations.DOWNLOAD.name(),
                Operations.DOWNLOAD_STREAM.name(),
                Operations.DELETE.name());
    }

    @Override
    public Set<String> supportedSchemes() {
        return Set.of("http", "https");
    }

    protected void checkGuard(final URI uri) {
        if (!uriGuard.test(uri)) {
            throw new ConnectorException("URI blocked by UriGuard: " + uri);
        }
    }

    protected abstract ConnectorResponse<Void> doDelete(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<byte[]> doDownload(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<InputStream> doDownloadStream(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<Boolean> doExists(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<Map<String, List<String>>> doHead(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<Void> doUpload(ConnectorRequest<?> request);

    protected abstract ConnectorResponse<Long> doUploadStream(ConnectorRequest<?> request);

    protected CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    protected Logger getLogger() {
        return logger;
    }
}
