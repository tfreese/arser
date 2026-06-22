// Created: 22.07.23
package de.freese.arser.core.component;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class DefaultComponentRegistry implements ComponentRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultComponentRegistry.class);

    private final CopyOnWriteArrayList<Lifecycle> components = new CopyOnWriteArrayList<>();

    @Override
    public void register(final Lifecycle component) {
        Objects.requireNonNull(component, "component required");

        final boolean added = components.addIfAbsent(component);

        if (added) {
            LOGGER.trace("Added: {}", component);
        }
    }

    @Override
    public int size() {
        return components.size();
    }

    @Override
    public synchronized void start() throws Exception {
        final int count = size();

        LOGGER.info("Starting {} components", count);

        for (final Lifecycle component : components) {
            component.start();
        }
    }

    public synchronized void stop() throws Exception {
        final int count = size();

        LOGGER.info("Stopping {} components in reverse order", count);

        for (final Lifecycle component : components.reversed()) {
            component.stop();
        }
    }

    @Override
    public String toString() {
        return ArserUtils.SERVER_NAME;
    }
}
