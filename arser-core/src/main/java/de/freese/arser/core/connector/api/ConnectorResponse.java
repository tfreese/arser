package de.freese.arser.core.connector.api;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * @author Thomas Freese
 */
public final class ConnectorResponse<R> {
    private final Map<String, Object> metadata;
    private final Instant timestamp;
    private final R value;

    public ConnectorResponse(final R value, final Map<String, Object> metadata, final Clock clock) {
        super();

        this.value = value;
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata));
        this.timestamp = clock.instant();
    }

    public ConnectorResponse(final R value, final Map<String, Object> metadata) {
        this(value, metadata, Clock.systemUTC());
    }

    public Map<String, Object> meta() {
        return metadata;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public R value() {
        return value;
    }
}
