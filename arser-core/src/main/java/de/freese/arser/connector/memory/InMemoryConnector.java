package de.freese.arser.connector.memory;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.core.Attributes;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.connector.spi.NotFoundException;
import de.freese.arser.connector.spi.UnsupportedOperationForSchemeException;

/**
 * @author Thomas Freese
 */
public final class InMemoryConnector implements Connector {
    private final Map<URI, byte[]> store = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        final URI uri = request.uri();

        return (ConnectorResponse<R>) switch (request.operation().name()) {
            case "exists" -> new ConnectorResponse<>(store.containsKey(uri), Map.of());
            case "download" -> {
                final byte[] data = store.get(uri);

                if (data == null) {
                    throw new NotFoundException("mem: " + uri);
                }

                yield new ConnectorResponse<>(data.clone(), Map.of("size", data.length));
            }
            case "upload" -> {
                store.put(uri, request.attribute(Attributes.BODY).orElseThrow().clone());

                yield new ConnectorResponse<>(null, Map.of());
            }
            case "delete" -> {
                final boolean removed = store.remove(uri) != null;

                yield new ConnectorResponse<>(null, Map.of("removed", removed));
            }
            default -> throw new UnsupportedOperationForSchemeException(request.operation().name(), "mem");
        };
    }

    public InMemoryConnector put(final URI uri, final byte[] data) {
        store.put(uri.normalize(), data.clone());

        return this;
    }

    @Override
    public Set<String> supportedOperations() {
        return Set.of("exists", "download", "upload", "delete");
    }

    @Override
    public Set<String> supportedSchemes() {
        return Set.of("mem");
    }
}
