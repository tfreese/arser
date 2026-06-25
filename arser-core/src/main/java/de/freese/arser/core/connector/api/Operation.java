package de.freese.arser.core.connector.api;

import java.util.Set;

/**
 * @author Thomas Freese
 */
public interface Operation<R> {
    default boolean isIdempotent() {
        return false;
    }

    default boolean isReadOnly() {
        return false;
    }

    String name();

    default Set<AttributeKey<?>> requiredAttributes() {
        return Set.of();
    }

    Class<R> resultType();
}
