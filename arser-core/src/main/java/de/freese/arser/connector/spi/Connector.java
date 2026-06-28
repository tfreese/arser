package de.freese.arser.connector.spi;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import de.freese.arser.component.Lifecycle;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.api.Operation;
import de.freese.arser.connector.api.Result;

/**
 * @author Thomas Freese
 */
public interface Connector extends Lifecycle {
    <R> ConnectorResponse<R> execute(ConnectorRequest<R> request);

    default <R> CompletableFuture<ConnectorResponse<R>> executeAsync(final ConnectorRequest<R> request) {
        return CompletableFuture.supplyAsync(() -> execute(request));
    }

    default <R> Result<R> safeExecute(final ConnectorRequest<R> request) {
        try {
            return new Result.Success<>(execute(request));
        }
        catch (NotFoundException _) {
            return new Result.NotFound<>(request.uri());
        }
        catch (final Throwable t) {
            return new Result.Failure<>(t);
        }
    }

    @Override
    default void start() throws Exception {
        // Empty
    }

    @Override
    default void stop() throws Exception {
        // Empty
    }

    Set<Operation<?>> supportedOperations();

    Set<String> supportedSchemes();

    default boolean supports(final String scheme, final Operation<?> operation) {
        return supportedSchemes().contains(scheme.toLowerCase()) && supportedOperations().contains(operation);
    }
}
