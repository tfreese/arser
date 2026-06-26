package de.freese.arser.connector.api;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Thomas Freese
 */
public final class ConnectorRequest<R> {
    public static <R> ConnectorRequest<R> of(final URI uri, final Operation<R> op) {
        return new ConnectorRequest<>(uri, op, Map.of());
    }

    private final Map<AttributeKey<?>, Object> attributes;
    private final Operation<R> operation;
    private final URI uri;

    private ConnectorRequest(final URI uri, final Operation<R> op, final Map<AttributeKey<?>, Object> attrs) {
        super();

        this.uri = Objects.requireNonNull(uri, "uri").normalize();
        this.operation = Objects.requireNonNull(op, "operation");
        this.attributes = Map.copyOf(attrs);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> attribute(final AttributeKey<T> key) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    public <T> T attributeOrDefault(final AttributeKey<T> key, final T defaultValue) {
        return attribute(key).orElse(defaultValue);
    }

    public Map<AttributeKey<?>, Object> attributes() {
        return attributes;
    }

    public Operation<R> operation() {
        return operation;
    }

    public URI uri() {
        return uri;
    }

    public <T> ConnectorRequest<R> with(final AttributeKey<T> key, final T value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        final Map<AttributeKey<?>, Object> copy = new HashMap<>(attributes);
        copy.put(key, value);

        return new ConnectorRequest<>(uri, operation, copy);
    }
}
