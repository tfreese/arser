// Created: 22.07.23
package de.freese.arser.core.server;

import de.freese.arser.config.ServerConfig;
import de.freese.arser.core.Arser;
import de.freese.arser.core.lifecycle.Lifecycle;

/**
 * @author Thomas Freese
 */
public interface ArserServer extends Lifecycle {
    ArserServer setArser(Arser arser);

    ArserServer setConfig(ServerConfig serverConfig);
}
