package de.freese.arser.component;

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
        register(new Lifecycle() {
            @Override
            public void start() {
                // Empty
            }

            @Override
            public void stop() throws Exception {
                stopable.stop();
            }
        });
    }

    default void register(final AutoCloseable autoCloseable) {
        register(new Lifecycle() {
            @Override
            public void start() {
                // Empty
            }

            @Override
            public void stop() throws Exception {
                autoCloseable.close();
            }
        });
    }

    int size();

    void start() throws Exception;

    void stop() throws Exception;
}
