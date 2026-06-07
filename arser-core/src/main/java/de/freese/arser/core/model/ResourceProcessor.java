package de.freese.arser.core.model;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface ResourceProcessor {
    void process(DataContainer dataContainer) throws Exception;
}
