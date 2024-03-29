// Created: 22.07.23
package de.freese.arser.core.lifecycle;

/**
 * @author Thomas Freese
 */
public interface Lifecycle {

    void start() throws Exception;

    void stop() throws Exception;
}
