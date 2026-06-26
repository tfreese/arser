package de.freese.arser.connector.decorator;

import java.net.HttpRetryException;
import java.time.Duration;
import java.util.Optional;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RetryPolicy;
import dev.failsafe.function.CheckedSupplier;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.spi.Connector;

/**
 * @author Thomas Freese
 */
public final class RetryingConnector extends AbstractConnectorDecorator {
    private final FailsafeExecutor<Object> failsafeExecutor;

    public RetryingConnector(final Connector delegate, final int maxRetries, final Duration retryInterval) {
        super(delegate);

        final RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
                .withMaxRetries(maxRetries)
                // .withDelay(retryInterval)
                .withBackoff(retryInterval, Duration.ofSeconds(30), 1.5D)
                .onRetry(event -> {
                    final Throwable lastException = event.getLastException();

                    if (lastException instanceof final HttpRetryException httpRetryException) {
                        getDelegateLogger().warn("Retry: {} - HTTP {} - {} - {}",
                                event.getExecutionCount(),
                                httpRetryException.responseCode(),
                                httpRetryException.getMessage(),
                                httpRetryException.getLocation()
                        );
                    } else if (lastException != null) {
                        final String error = Optional.ofNullable(lastException.getMessage()).orElse(lastException.getClass().getSimpleName());
                        getDelegateLogger().warn("retry: {} - {}", event.getExecutionCount(), error);
                    } else {
                        getDelegateLogger().warn("retry: {}", event.getExecutionCount());
                    }
                })
                .onFailure(event -> {
                    final Throwable throwable = event.getException();

                    if (throwable != null) {
                        getDelegateLogger().error(throwable.getMessage(), throwable);
                    } else {
                        getDelegateLogger().error(event.toString());
                    }
                })
                .build();

        failsafeExecutor = Failsafe.with(retryPolicy);
    }

    @Override
    public <R> ConnectorResponse<R> execute(final ConnectorRequest<R> request) {
        if (!request.operation().isIdempotent()) {
            return super.execute(request);
        }

        final CheckedSupplier<ConnectorResponse<R>> checkedSupplier = () -> super.execute(request);

        return failsafeExecutor.get(checkedSupplier);
    }
}
