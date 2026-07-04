package de.freese.arser.component;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 * @since 22.07.23
 */
public abstract class AbstractComponent implements Lifecycle {
    protected enum State {
        NEW,
        RUNNING,
        STOPPED
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    @Override
    public final void start() throws Exception {
        if (!state.compareAndSet(State.NEW, State.RUNNING)) {
            getLogger().debug("start() ignored — current state is: {}", state.get());
            return;
        }

        getLogger().info("starting: {}", this);

        doStart();

        state.set(State.RUNNING);
    }

    @Override
    public final void stop() throws Exception {
        if (!state.compareAndSet(State.RUNNING, State.STOPPED)) {
            getLogger().debug("stop() ignored — current state is: {}", state.get());
            return;
        }

        getLogger().info("stopping: {}", this);

        doStop();
    }

    protected abstract void doStart() throws Exception;

    protected abstract void doStop() throws Exception;

    protected Logger getLogger() {
        return logger;
    }
}
