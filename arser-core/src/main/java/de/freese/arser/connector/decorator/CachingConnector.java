package de.freese.arser.connector.decorator;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
public final class CachingConnector extends AbstractConnectorDecorator {
    private record Entry(ConnectorResponse<?> response, Instant expiresAt) {
    }

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private final Duration ttl;

    public CachingConnector(final Connector delegate, final Duration ttl) {
        super(delegate);

        this.ttl = Objects.requireNonNull(ttl, "ttl required");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        final String key = "%s: %s".formatted(request.operation().name(), request.uri());

        if (!request.operation().isReadOnly()) {
            cache.remove(key);

            return super.execute(request);
        }

        final Entry entry = cache.get(key);

        if (entry != null && entry.expiresAt().isAfter(Instant.now())) {
            return (ConnectorResponse<R>) entry.response();
        }

        final ConnectorResponse<R> response = super.execute(request);

        cache.put(key, new Entry(response, Instant.now().plus(ttl)));

        return response;
    }
}
