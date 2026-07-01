package de.freese.arser.utils;

import de.freese.arser.component.LifeCycleRegistry;

/**
 * @author Thomas Freese
 */
public abstract class AbstractBuilder<B, T> {
    public abstract T build(LifeCycleRegistry lifeCycleRegistry) throws Exception;

    protected abstract B self();
}
