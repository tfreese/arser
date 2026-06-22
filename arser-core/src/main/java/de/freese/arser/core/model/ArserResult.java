package de.freese.arser.core.model;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public sealed interface ArserResult<R> {
    record Failure<R>(Throwable cause) implements ArserResult<R> {
    }

    record NotFound<R>(URI uri) implements ArserResult<R> {
    }

    record Success<R>(BlobValue blobValue) implements ArserResult<R> {
    }

    default boolean isSuccess() {
        return this instanceof Success<R>;
    }
}
