package de.freese.arser.api;

import java.net.URI;

import de.freese.arser.blobvalue.BlobValue;

/**
 * @author Thomas Freese
 */
public sealed interface ArserResult<R> {
    record Download<R>(BlobValue blobValue) implements ArserResult<R> {
    }

    record Exist<R>(URI uri, boolean exist) implements ArserResult<R> {
    }

    record Failure<R>(Throwable cause) implements ArserResult<R> {
    }

    record Forbidden<R>(URI uri) implements ArserResult<R> {
    }

    record NotFound<R>(URI uri) implements ArserResult<R> {
    }

    record Upload<R>(long contentLength) implements ArserResult<R> {
    }

    // default boolean isSuccess() {
    //     return this instanceof ArserResult.Download<R>
    //             || this instanceof ArserResult.Upload<R>;
    // }
}
