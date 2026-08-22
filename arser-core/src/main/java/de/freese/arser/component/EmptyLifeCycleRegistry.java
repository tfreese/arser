package de.freese.arser.component;

/**
 * @author Thomas Freese
 * @since 22.08.26
 */
public final class EmptyLifeCycleRegistry implements LifeCycleRegistry {
    public static final EmptyLifeCycleRegistry INSTANCE = new EmptyLifeCycleRegistry();

    private EmptyLifeCycleRegistry() {
        super();
    }

    @Override
    public void register(final Lifecycle lifecycle) {
        // Empty
    }

    @Override
    public void register(final Startable startable) {
        // Empty
    }

    @Override
    public void register(final Stopable stopable) {
        // Empty
    }

    @Override
    public void register(final AutoCloseable autoCloseable) {
        // Empty
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void start() throws Exception {
        // Empty
    }

    @Override
    public void stop() throws Exception {
        // Empty
    }
}
