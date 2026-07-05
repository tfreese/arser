package de.freese.arser.repository;

import java.io.InputStream;
import java.net.URI;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.component.Lifecycle;

/**
 * @author Thomas Freese
 * @since 19.07.23
 */
public interface Repository extends Lifecycle {

    ArserResult download(ArserRequest arserRequest);

    ArserResult exist(ArserRequest arserRequest);

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

    default ArserResult upload(final ArserRequest arserRequest, final InputStream inputStream) {
        final String message = "repository is read only: %s [%s]".formatted(getName(), getClass().getSimpleName());

        return new ArserResult.Forbidden(arserRequest.getResource(), message);
    }
}
