package de.freese.arser.connector.observability;

import java.time.Duration;

import de.freese.arser.connector.api.ConnectorRequest;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface MetricsRecorder {
    MetricsRecorder NOOP = (r, e, o) -> {
    };

    void record(ConnectorRequest<?> req, Duration elapsed, String outcome);
}
