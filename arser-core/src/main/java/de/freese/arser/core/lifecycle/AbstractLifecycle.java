// Created: 22.07.23
package de.freese.arser.core.lifecycle;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public abstract class AbstractLifecycle implements Lifecycle {
    enum State {
        NEW,
        STARTED,
        STOPPED,
        FAILED
    }

    private final Lock lock = new ReentrantLock();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private volatile State currentState = State.NEW;

    @Override
    public final void start() throws Exception {
        if (isStarted()) {
            return;
        }

        ensure(Set.of(State.NEW, State.STOPPED)); // check state before taking lock

        lock.lock();

        try {
            ensure(Set.of(State.NEW, State.STOPPED)); // check again now we have lock

            try {
                getLogger().info("Starting: {}", this);

                doStart();

                currentState = State.STARTED;
                getLogger().info("Started: {}", this);
            }
            catch (Exception ex) {
                doFailed("start", ex);
            }
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public final void stop() throws Exception {
        if (isStopped()) {
            return;
        }

        ensure(Set.of(State.STARTED)); // check state before taking lock

        lock.lock();

        try {
            ensure(Set.of(State.STARTED)); // check again now we have lock

            try {
                getLogger().info("Stopping: {}", this);

                doStop();

                currentState = State.STOPPED;

                getLogger().info("Stopped {}", this);
            }
            catch (Exception ex) {
                doFailed("stop", ex);
            }
        }
        finally {
            lock.unlock();
        }
    }

    protected void doFailed(final String operation, final Exception ex) {
        getLogger().error("Lifecycle operation '%s' failed".formatted(operation), ex);

        currentState = State.FAILED;

        if (ex instanceof RuntimeException rex) {
            throw rex;
        }
        else {
            throw new RuntimeException(ex);
        }
    }

    protected abstract void doStart() throws Exception;

    protected abstract void doStop() throws Exception;

    protected Logger getLogger() {
        return logger;
    }

    protected boolean isFailed() {
        return is(State.FAILED);
    }

    protected boolean isStarted() {
        return is(State.STARTED);
    }

    protected boolean isStopped() {
        return is(State.STOPPED);
    }

    private void ensure(final Set<State> allowed) {
        for (State allow : allowed) {
            if (is(allow)) {
                return;
            }
        }

        throw new IllegalStateException("Invalid state: " + currentState + "; allowed: " + allowed.stream().map(State::name).collect(Collectors.joining(",")));
    }

    private boolean is(final State state) {
        return currentState == state;
    }
}
