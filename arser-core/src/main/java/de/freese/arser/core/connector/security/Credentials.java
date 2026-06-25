package de.freese.arser.core.connector.security;

import javax.net.ssl.SSLContext;

/**
 * @author Thomas Freese
 */
public sealed interface Credentials {
    record Basic(String user, String password) implements Credentials {
    }

    record Bearer(String token) implements Credentials {
    }

    record MTLS(SSLContext context) implements Credentials {
    }
}
