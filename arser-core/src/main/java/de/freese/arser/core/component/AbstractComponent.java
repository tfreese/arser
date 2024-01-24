// Created: 22.07.23
package de.freese.arser.core.component;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public abstract class AbstractComponent {

    protected static <T> T assertNotNull(final T value, final Supplier<String> postfixSupplier) {
        return Objects.requireNonNull(value, () -> postfixSupplier.get() + " required");
    }

    protected static <T> T assertValue(final T value, final Predicate<T> predicate, final Supplier<String> messageSupplier) {
        if (predicate.test(value)) {
            final String message = messageSupplier.get();

            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException();
            }
            else if (message.endsWith(":")) {
                throw new IllegalArgumentException(message + " " + value);
            }
            else {
                throw new IllegalArgumentException(message + ": " + value);
            }
        }

        return value;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected Logger getLogger() {
        return logger;
    }
}
