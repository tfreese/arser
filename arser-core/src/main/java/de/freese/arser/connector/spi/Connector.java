package de.freese.arser.connector.spi;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.api.ConnectorResponse;
import de.freese.arser.connector.api.Result;

/**
 * @author Thomas Freese
 */
public interface Connector extends AutoCloseable {
    @Override
    default void close() {
        // Empty
    }

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

    Set<String> supportedOperations();

    Set<String> supportedSchemes();

    default boolean supports(final String scheme, final String operation) {
        return supportedSchemes().contains(scheme.toLowerCase()) && supportedOperations().contains(operation);
    }
}
