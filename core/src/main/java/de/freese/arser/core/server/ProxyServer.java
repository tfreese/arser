// Created: 22.07.23
package de.freese.arser.core.server;

import de.freese.arser.config.ServerConfig;
import de.freese.arser.core.lifecycle.Lifecycle;
import de.freese.arser.core.repository.Repository;

/**
 * @author Thomas Freese
 */
public interface ProxyServer extends Lifecycle {

    ProxyServer addContextRoot(String contextRoot, Repository repository);

    ProxyServer setConfig(ServerConfig serverConfig);
}
