// Created: 19.07.23
package de.freese.arser.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.component.Lifecycle;
import de.freese.arser.model.ArserRequest;
import de.freese.arser.model.ArserResult;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    <R> ArserResult<R> download(ArserRequest arserRequest);

    <R> ArserResult<R> exist(ArserRequest arserRequest);

    String getName();

    URI getUri();

    @Override
    default void start() throws Exception {
        // Empty
    }

    @Override
    default void stop() throws Exception {
        // Empty
    }

    default <R> ArserResult<R> upload(final ArserRequest arserRequest, final InputStream inputStream) {
        final String message = "repository is read only: %s [%s]".formatted(getName(), getClass().getSimpleName());

        return new ArserResult.Failure<>(new UnsupportedOperationException(message));
    }
}
