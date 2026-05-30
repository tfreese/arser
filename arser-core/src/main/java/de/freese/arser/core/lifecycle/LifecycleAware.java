// Created: 22.07.23
package de.freese.arser.core.lifecycle;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface LifecycleAware {
    Lifecycle getLifecycle();
}
