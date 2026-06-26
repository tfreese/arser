package de.freese.arser.connector.observability;

import de.freese.arser.connector.api.ConnectorRequest;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface Tracer {
    Tracer NOOP = req -> new Span() {
        public void close() {
            // Empty
        }

        public void recordException(final Throwable th) {
            // Empty
        }

        public void setAttribute(final String key, final Object value) {
            // Empty
        }
    };

    interface Span extends AutoCloseable {
        @Override
        void close();

        void recordException(Throwable th);

        void setAttribute(String key, Object value);
    }

    Span start(ConnectorRequest<?> req);
}
