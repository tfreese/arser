package de.freese.arser.api;

import java.net.URI;

import de.freese.arser.blobvalue.BlobValue;

/**
 * @author Thomas Freese
 */
public sealed interface ArserResult {
    record Download(BlobValue blobValue) implements ArserResult {
    }

    record Exist(URI uri) implements ArserResult {
    }

    record Failure(Throwable cause) implements ArserResult {
    }

    record Forbidden(URI uri, String reason) implements ArserResult {
    }

    record NotFound(URI uri) implements ArserResult {
    }

    record Upload(long contentLength) implements ArserResult {
    }

    // default boolean isSuccess() {
    //     return this instanceof ArserResult.Download
    //             || this instanceof ArserResult.Upload;
    // }
}
