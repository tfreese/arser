package de.freese.arser.component;

import java.util.Objects;

/**
 * @author Thomas Freese
 */
public interface LifeCycleRegistry {
    void register(Lifecycle lifecycle);

    default void register(final Startable startable) {
        register(new Lifecycle() {
            @Override
            public void start() throws Exception {
                startable.start();
            }

            @Override
            public void stop() {
                // Empty
            }
        });
    }

    default void register(final Stopable stopable) {
        Objects.requireNonNull(stopable, "stopable required");

        register(new Lifecycle() {
            @Override
            public void start() {
                // Empty
            }

            @Override
            public void stop() throws Exception {
                stopable.stop();
            }

            @Override
            public String toString() {
                return stopable.toString();
            }
        });
    }

    default void register(final AutoCloseable autoCloseable) {
        Objects.requireNonNull(autoCloseable, "autoCloseable required");

        register(new Lifecycle() {
            @Override
            public void start() {
                // Empty
            }

            @Override
            public void stop() throws Exception {
                autoCloseable.close();
            }

            @Override
            public String toString() {
                return autoCloseable.toString();
            }
        });
    }

    int size();

    void start() throws Exception;

    void stop() throws Exception;
}
