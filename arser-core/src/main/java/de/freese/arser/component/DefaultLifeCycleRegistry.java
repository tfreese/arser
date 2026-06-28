// Created: 22.07.23
package de.freese.arser.component;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public class DefaultLifeCycleRegistry implements LifeCycleRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLifeCycleRegistry.class);

    private final CopyOnWriteArrayList<Lifecycle> lifecycles = new CopyOnWriteArrayList<>();

    @Override
    public void register(final Lifecycle lifecycle) {
        Objects.requireNonNull(lifecycle, "lifecycle required");

        final boolean added = lifecycles.addIfAbsent(lifecycle);

        if (added) {
            LOGGER.trace("Added: {}", lifecycle);
        }
    }

    @Override
    public int size() {
        return lifecycles.size();
    }

    @Override
    public synchronized void start() throws Exception {
        final int count = size();

        LOGGER.info("Starting {} lifecycles", count);

        for (final Lifecycle lifecycle : lifecycles) {
            lifecycle.start();
        }
    }

    public synchronized void stop() throws Exception {
        final int count = size();

        LOGGER.info("Stopping {} lifecycles in reverse order", count);

        for (final Lifecycle lifecycle : lifecycles.reversed()) {
            lifecycle.stop();
        }
    }
}
