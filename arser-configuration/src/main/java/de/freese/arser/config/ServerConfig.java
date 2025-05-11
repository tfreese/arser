// Created: 31 Okt. 2024
package de.freese.arser.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public final class ServerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfig.class);

    public static final class Builder {
        private int port;
        private ThreadPoolConfig threadPoolConfig;

        private Builder() {
            super();
        }

        public ServerConfig build() {
            ConfigValidator.value(port, value -> value >= 1025 && value <= 65534, () -> "port not in range 1025-65534: %d".formatted(port));

            if (threadPoolConfig == null) {
                threadPoolConfig = ThreadPoolConfig.builderServerDefault().build();

                LOGGER.info("threadPoolConfig not set, using default: {}", threadPoolConfig);
            }

            return new ServerConfig(this);
        }

        public Builder port(final Integer port) {
            if (port == null) {
                return this;
            }

            this.port = port;

            return this;
        }

        public Builder threadPoolConfig(final ThreadPoolConfig threadPoolConfig) {
            this.threadPoolConfig = threadPoolConfig;

            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final int port;
    private final ThreadPoolConfig threadPoolConfig;

    private ServerConfig(final Builder builder) {
        super();

        port = builder.port;
        threadPoolConfig = builder.threadPoolConfig;
    }

    public int getPort() {
        return port;
    }

    public ThreadPoolConfig getThreadPoolConfig() {
        return threadPoolConfig;
    }
}
