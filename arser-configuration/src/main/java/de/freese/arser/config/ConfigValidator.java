// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * @author Thomas Freese
 */
public final class ConfigValidator {
    private static final Pattern PATTERN_CONTEXT_ROOT = Pattern.compile("([a-z0-9\\-_])+");

    public static void contextRoot(final String contextRoot) {
        string(contextRoot, () -> "contextRoot required: '%s'".formatted(contextRoot));

        if (!PATTERN_CONTEXT_ROOT.matcher(contextRoot).matches()) {
            throw new IllegalArgumentException("contextRoot must match the pattern: " + PATTERN_CONTEXT_ROOT);
        }
    }

    public static <T> void notNull(final T value, final Supplier<String> messageSupplier) {
        if (value == null) {
            final String message = messageSupplier.get();

            throw new IllegalArgumentException(message);
        }
    }

    public static void string(final String value, final Supplier<String> messageSupplier) {
        value(value,
                v -> v != null
                        && !v.isBlank(),
                messageSupplier);
    }

    public static void uri(final URI uri) {
        value(uri,
                value -> value != null
                        && ("file".equalsIgnoreCase(uri.getScheme())
                        || "http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme())
                ),
                () -> "uri required or invalid protocol: '%s'".formatted(uri));
    }

    public static <T> void value(final T value, final Predicate<T> predicate, final Supplier<String> messageSupplier) {
        if (!predicate.test(value)) {
            final String message = messageSupplier.get();

            throw new IllegalArgumentException(message);
        }
    }

    private ConfigValidator() {
        super();
    }
}
