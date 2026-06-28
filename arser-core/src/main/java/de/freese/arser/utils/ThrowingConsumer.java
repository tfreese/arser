// Created: 16.02.2017
package de.freese.arser.utils;

import java.util.Objects;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T t) throws E;

    default ThrowingConsumer<T, E> andThen(final ThrowingConsumer<? super T, E> after) {
        Objects.requireNonNull(after);

        return t -> {
            accept(t);
            after.accept(t);
        };
    }
}
