// Created: 22.07.23
package de.freese.arser.core.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class LifecycleManager extends AbstractLifecycle {

    private final CopyOnWriteArrayList<Lifecycle> components = new CopyOnWriteArrayList<>();

    public void add(final Lifecycle component) {
        assertNotNull(component, () -> "Lifecycle");

        final boolean added = components.addIfAbsent(component);

        if (added) {
            getLogger().trace("Added: {}", component);
        }
    }

    public void add(final LifecycleAware component) {
        assertNotNull(component, () -> "LifecycleAware");

        add(component.getLifecycle());
    }

    public void clear() {
        components.clear();

        getLogger().trace("Cleared");
    }

    public void remove(final Lifecycle component) {
        assertNotNull(component, () -> "Lifecycle");

        final boolean removed = components.remove(component);

        if (removed) {
            getLogger().trace("Removed: {}", component);
        }
    }

    public void remove(final LifecycleAware component) {
        assertNotNull(component, () -> "LifecycleAware");

        remove(component.getLifecycle());
    }

    public int size() {
        return components.size();
    }

    @Override
    public String toString() {
        return ArserUtils.SERVER_NAME;
    }

    @Override
    protected void doStart() throws Exception {
        final int count = components.size();

        getLogger().info("Starting {} components", count);

        final List<Throwable> throwables = new ArrayList<>(count);

        for (Lifecycle component : components) {
            try {
                component.start();
            }
            catch (Throwable failure) {
                getLogger().error("Failed to start component: " + component, failure);
                throwables.add(failure);
            }
        }

        maybePropagate(throwables, "start");
    }

    @Override
    protected void doStop() throws Exception {
        final int count = components.size();

        getLogger().info("Stopping {} components", count);

        final List<Throwable> throwables = new ArrayList<>(count);

        for (Lifecycle component : components.reversed()) {
            try {
                component.stop();
            }
            catch (Throwable failure) {
                getLogger().error("Failed to stop component: " + component, failure);
                throwables.add(failure);
            }
        }

        maybePropagate(throwables, "stop");
    }

    protected void maybePropagate(final List<Throwable> throwables, final String messagePart) {
        if (throwables.isEmpty()) {
            return;
        }

        final String message = "Failed to %s %d components".formatted(messagePart, throwables.size());

        getLogger().error(message);

        if ("start".equalsIgnoreCase(messagePart)) {
            throw new IllegalStateException(message);
        }
    }
}
