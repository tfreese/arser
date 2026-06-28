package de.freese.arser.component;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface Stopable {
    void stop() throws Exception;
}
