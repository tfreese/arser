// Created: 19.07.23
package de.freese.arser.core.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.core.component.Lifecycle;
import de.freese.arser.core.model.ArserRequest;
import de.freese.arser.core.model.ArserResult;

/**
 * @author Thomas Freese
 */
public interface Repository extends Lifecycle {

    <R> ArserResult<R> exist(ArserRequest arserRequest);

    String getName();

    <R> ArserResult<R> getResource(ArserRequest arserRequest);

    URI getUri();

    default <R> ArserResult<R> upload(final ArserRequest arserRequest, final InputStream inputStream) {
        final String message = "repository is read only: %s [%s]".formatted(getName(), getClass().getSimpleName());

        return new ArserResult.Failure<>(new UnsupportedOperationException(message));
    }
}
