// Created: 11 Mai 2025
package de.freese.arser.spring;

import de.freese.arser.config.ArserConfig;
import de.freese.arser.instance.AbstractArserInstance;

/**
 * @author Thomas Freese
 */
public final class SpringArserInstance extends AbstractArserInstance {
    public SpringArserInstance(final String name, final ArserConfig config) {
        super(name, config);
    }

    @Override
    public void shutdown() {
        getLogger().info("shutdown instance (lifecycle handled by spring-framework): {}", getName());
    }
}
