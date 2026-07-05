package de.freese.arser.repository.decorator;

import java.net.HttpRetryException;
import java.time.Duration;
import java.util.Optional;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RetryPolicy;
import dev.failsafe.function.CheckedSupplier;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 */
public final class RetryingRepositoryDecorator extends AbstractRepositoryDecorator {
    private final FailsafeExecutor<Object> failsafeExecutor;

    public RetryingRepositoryDecorator(final Repository delegate, final int maxRetries, final Duration retryInterval) {
        super(delegate);

        final RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
                .withMaxRetries(maxRetries)
                // .withDelay(retryInterval)
                .withBackoff(retryInterval, Duration.ofSeconds(30), 1.5D)
                .onRetry(event -> {
                    final Throwable lastException = event.getLastException();

                    if (lastException instanceof final HttpRetryException httpRetryException) {
                        getLogger().warn("Retry: {} - HTTP {} - {} - {}",
                                event.getExecutionCount(),
                                httpRetryException.responseCode(),
                                httpRetryException.getMessage(),
                                httpRetryException.getLocation()
                        );
                    } else if (lastException != null) {
                        final String error = Optional.ofNullable(lastException.getMessage()).orElse(lastException.getClass().getSimpleName());
                        getLogger().warn("retry: {} - {}", event.getExecutionCount(), error);
                    } else {
                        getLogger().warn("retry: {}", event.getExecutionCount());
                    }
                })
                .onFailure(event -> {
                    final Throwable throwable = event.getException();

                    if (throwable != null) {
                        getLogger().error(throwable.getMessage(), throwable);
                    } else {
                        getLogger().error(event.toString());
                    }
                })
                .build();

        failsafeExecutor = Failsafe.with(retryPolicy);
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final CheckedSupplier<ArserResult> checkedSupplier = () -> super.download(arserRequest);

        return failsafeExecutor.get(checkedSupplier);
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final CheckedSupplier<ArserResult> checkedSupplier = () -> super.exist(arserRequest);

        return failsafeExecutor.get(checkedSupplier);
    }

    // @Override
    // public ArserResult upload(final ArserRequest arserRequest, final InputStream inputStream) {
    //     final CheckedSupplier<ArserResult> checkedSupplier = () -> super.upload(arserRequest);
    //
    //     return failsafeExecutor.get(checkedSupplier);
    // }
}
