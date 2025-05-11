// Created: 11 Mai 2025
package de.freese.arser.instance;

import java.util.Objects;

import de.freese.arser.config.ArserConfig;
import de.freese.arser.core.lifecycle.LifecycleManager;

/**
 * @author Thomas Freese
 */
final class DefaultArserInstance extends AbstractArserInstance {
    private final LifecycleManager lifecycleManager;

    DefaultArserInstance(final String name, final ArserConfig config, final LifecycleManager lifecycleManager) {
        super(name, config);

        this.lifecycleManager = Objects.requireNonNull(lifecycleManager, "lifecycleManager required");
    }

    @Override
    public void shutdown() {
        getLogger().info("shutdown instance: {}", getName());

        try {
            lifecycleManager.stop();
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }
}
