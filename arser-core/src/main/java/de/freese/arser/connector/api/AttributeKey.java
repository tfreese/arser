package de.freese.arser.connector.api;

import java.util.Objects;

/**
 * @author Thomas Freese
 */
public final class AttributeKey<T> {
    public static <T> AttributeKey<T> of(final String name, final Class<T> type) {
        return new AttributeKey<>(name, type);
    }

    private final String name;
    private final Class<T> type;

    private AttributeKey(final String name, final Class<T> type) {
        super();

        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof final AttributeKey<?> k && name.equals(k.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "AttributeKey[" + name + ":" + type.getSimpleName() + "]";
    }

    public Class<T> type() {
        return type;
    }
}
