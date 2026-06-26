package de.freese.arser.connector.api;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public sealed interface Result<R> {
    record Failure<R>(Throwable cause) implements Result<R> {
    }

    record NotFound<R>(URI uri) implements Result<R> {
    }

    record Success<R>(ConnectorResponse<R> response) implements Result<R> {
    }

    default boolean isSuccess() {
        return this instanceof Success<R>;
    }
}
