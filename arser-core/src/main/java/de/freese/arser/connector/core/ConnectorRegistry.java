package de.freese.arser.connector.core;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.api.Operation;
import de.freese.arser.connector.api.Result;
import de.freese.arser.connector.observability.MetricsRecorder;
import de.freese.arser.connector.observability.Tracer;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.connector.spi.ConnectorException;
import de.freese.arser.connector.spi.NotFoundException;
import de.freese.arser.connector.spi.UnsupportedOperationForSchemeException;

/**
 * @author Thomas Freese
 */
public final class ConnectorRegistry implements AutoCloseable {
    public static ConnectorRegistry autoDiscover() {
        final ConnectorRegistry registry = new ConnectorRegistry();

        ServiceLoader.load(Connector.class).forEach(registry::register);

        return registry;
    }

    private final List<Connector> connectors = new CopyOnWriteArrayList<>();
    private final MetricsRecorder metrics;
    private final Tracer tracer;

    public ConnectorRegistry() {
        this(MetricsRecorder.NOOP, Tracer.NOOP);
    }

    public ConnectorRegistry(final MetricsRecorder metrics, final Tracer tracer) {
        super();

        this.metrics = Objects.requireNonNull(metrics, "metrics required");
        this.tracer = Objects.requireNonNull(tracer, "tracer required");
    }

    public <R> R call(final URI uri, final Operation<R> operation) {
        return execute(ConnectorRequest.of(uri, operation)).value();
    }

    @Override
    public void close() {
        final List<Connector> copy = new ArrayList<>(connectors);
        connectors.clear();

        Collections.reverse(copy);

        for (final Connector connector : copy) {
            try {
                connector.close();
            }
            catch (Exception _) {
                // Empty
            }
        }

        copy.clear();
    }

    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        Validation.validate(request);

        final long start = System.nanoTime();

        try (var span = tracer.start(request)) {
            final ConnectorResponse<R> response = select(request).execute(request);
            span.setAttribute("statusCode", response.meta().get("statusCode"));
            metrics.record(request, Duration.ofNanos(System.nanoTime() - start), "success");

            return response;
        }
        catch (final RuntimeException ex) {
            metrics.record(request, Duration.ofNanos(System.nanoTime() - start), "failure");
            throw ex;
        }
    }

    public <R> CompletableFuture<ConnectorResponse<R>> executeAsync(final ConnectorRequest<R> request) {
        Validation.validate(request);

        return select(request).executeAsync(request);
    }

    public ConnectorRegistry register(final Connector connector) {
        connectors.add(Objects.requireNonNull(connector));

        return this;
    }

    public <R> Result<R> safeExecute(final ConnectorRequest<R> request) {
        try {
            return new Result.Success<>(execute(request));
        }
        catch (NotFoundException _) {
            return new Result.NotFound<>(request.uri());
        }
        catch (final Throwable th) {
            return new Result.Failure<>(th);
        }
    }

    private <R> Connector select(final ConnectorRequest<R> request) {
        final String scheme = request.uri().getScheme();

        if (scheme == null) {
            throw new ConnectorException("URI without Schema: " + request.uri());
        }

        return connectors.stream()
                .filter(c -> c.supports(scheme, request.operation()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationForSchemeException(request.operation().name(), scheme));
    }
}
