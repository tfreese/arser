package de.freese.arser.repository.decorator;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.repository.Repository;

/**
 * @author Thomas Freese
 */
public final class CachingMemoryRepositoryDecorator extends AbstractRepositoryDecorator {
    private record Entry(ArserResult response, Instant expiresAt) {
    }

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private final Duration ttl;

    public CachingMemoryRepositoryDecorator(final Repository delegate, final Duration ttl) {
        super(delegate);

        this.ttl = Objects.requireNonNull(ttl, "ttl required");
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        return execute("download", arserRequest, super::download);
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        return execute("exist", arserRequest, super::exist);
    }

    private ArserResult execute(final String operation, final ArserRequest arserRequest, final Function<ArserRequest, ArserResult> function) {
        final String key = "%s-%s".formatted(operation, arserRequest.getResource());

        final Entry entry = cache.get(key);

        if (entry != null) {
            if (entry.expiresAt().isAfter(Instant.now())) {
                getLogger().debug("use cached entry for: {}", arserRequest);

                return entry.response();
            } else {
                getLogger().debug("remove expired cached entry for: {}", arserRequest);
                cache.remove(key);
            }
        }

        final ArserResult arserResult = function.apply(arserRequest);

        getLogger().debug("add cache entry for: {}", arserRequest);

        cache.put(key, new Entry(arserResult, Instant.now().plus(ttl)));

        return arserResult;
    }
}
