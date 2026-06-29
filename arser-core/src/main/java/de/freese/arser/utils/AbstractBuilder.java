package de.freese.arser.utils;

/**
 * @author Thomas Freese
 */
public abstract class AbstractBuilder<B, T> {
    public abstract T build() throws Exception;

    protected abstract B self();
}
