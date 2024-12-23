// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Thomas Freese
 */
public final class ConfigValidator {
    public static String contextRoot(final String name) {
        return string(name, () -> "contextRoot required: '%s'".formatted(name));
    }

    public static String string(final String value, final Supplier<String> messageSupplier) {
        return value(value,
                v -> v != null
                        && !v.isBlank()
                        && !v.isEmpty(),
                messageSupplier);
    }

    public static URI uri(final URI uri) {
        return value(uri,
                value -> value != null
                        && ("file".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())),
                () -> "uri required or invalid protocol: '%s'".formatted(uri));
    }

    public static <T> T value(final T value, final Predicate<T> predicate, final Supplier<String> messageSupplier) {
        if (!predicate.test(value)) {
            final String message = messageSupplier.get();

            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private ConfigValidator() {
        super();
    }
}
