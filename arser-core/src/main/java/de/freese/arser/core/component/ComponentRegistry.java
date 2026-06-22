package de.freese.arser.core.component;

/**
 * @author Thomas Freese
 */
public interface ComponentRegistry {
    void register(Lifecycle component);

    int size();

    void start() throws Exception;

    void stop() throws Exception;
}
