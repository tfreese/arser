// Created: 31 Okt. 2024
package de.freese.arser.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public final class HttpClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfig.class);

    public static final class Builder {
        private ThreadPoolConfig threadPoolConfig;

        private Builder() {
            super();
        }

        public HttpClientConfig build() {
            if (threadPoolConfig == null) {
                threadPoolConfig = ThreadPoolConfig.builderClientDefault().build();

                LOGGER.info("threadPoolConfig not set, using default: {}", threadPoolConfig);
            }

            return new HttpClientConfig(this);
        }

        public HttpClientConfig.Builder threadPoolConfig(final ThreadPoolConfig threadPoolConfig) {
            this.threadPoolConfig = threadPoolConfig;

            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ThreadPoolConfig threadPoolConfig;

    private HttpClientConfig(final Builder builder) {
        super();

        threadPoolConfig = builder.threadPoolConfig;
    }

    public ThreadPoolConfig getThreadPoolConfig() {
        return threadPoolConfig;
    }
}
