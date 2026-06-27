package de.freese.arser.core.model;

import java.net.URI;

import de.freese.arser.blobvalue.BlobValue;

/**
 * @author Thomas Freese
 */
public sealed interface ArserResult<R> {
    record Exist<R>(boolean exist) implements ArserResult<R> {
    }

    record Failure<R>(Throwable cause) implements ArserResult<R> {
    }

    record NotFound<R>(URI uri) implements ArserResult<R> {
    }

    record Resource<R>(BlobValue blobValue) implements ArserResult<R> {
    }

    record Uploaded<R>() implements ArserResult<R> {
    }

    default boolean isSuccess() {
        return this instanceof ArserResult.Resource<R>
                || this instanceof ArserResult.Uploaded<R>;
    }
}
