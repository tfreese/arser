package de.freese.arser.connector.memory;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Attributes;
import de.freese.arser.connector.core.Operations;
import de.freese.arser.connector.spi.AbstractConnector;
import de.freese.arser.connector.spi.NotFoundException;
import de.freese.arser.connector.spi.UnsupportedOperationForSchemeException;

/**
 * @author Thomas Freese
 */
public final class InMemoryConnector extends AbstractConnector {
    private final Map<URI, byte[]> store = new ConcurrentHashMap<>();

    public InMemoryConnector() {
        super(Set.of("mem"), Set.of(
                Operations.DELETE,
                Operations.DOWNLOAD,
                Operations.EXISTS,
                Operations.UPLOAD
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        final URI uri = request.uri();

        final ConnectorResponse<?> response;

        if (Operations.DELETE.equals(request.operation())) {
            final boolean removed = store.remove(uri) != null;

            response = new ConnectorResponse<>(null, Map.of("removed", removed));
        } else if (Operations.DOWNLOAD.equals(request.operation())) {
            final byte[] data = store.get(uri);

            if (data == null) {
                throw new NotFoundException("mem: " + uri);
            }

            response = new ConnectorResponse<>(data.clone(), Map.of("size", data.length));
        } else if (Operations.EXISTS.equals(request.operation())) {
            response = new ConnectorResponse<>(store.containsKey(uri), Map.of());
        } else if (Operations.UPLOAD.equals(request.operation())) {
            store.put(uri, request.attribute(Attributes.BODY).orElseThrow().clone());

            response = new ConnectorResponse<>(null, Map.of());
        } else {
            throw new UnsupportedOperationForSchemeException(request.operation().name(), request.uri().getScheme());
        }

        return (ConnectorResponse<R>) response;
    }

    public InMemoryConnector put(final URI uri, final byte[] data) {
        store.put(uri.normalize(), data.clone());

        return this;
    }
}
