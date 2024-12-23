// Created: 31 Okt. 2024
package de.freese.arser.config;

/**
 * @author Thomas Freese
 */
public final class ServerConfig {
    public static final class Builder {
        private int port;
        private String threadNamePattern;
        private int threadPoolCoreSize;
        private int threadPoolMaxSize;

        private Builder() {
            super();
        }

        public ServerConfig build() {
            ConfigValidator.value(port, value -> value >= 1025 && value <= 65534, () -> "port not in range 1025-65534: %d".formatted(port));
            ConfigValidator.string(threadNamePattern, () -> "threadNamePattern required: '%s'".formatted(threadNamePattern));
            ConfigValidator.value(threadPoolCoreSize, value -> value > 0, () -> "ThreadPoolCoreSize is <= 0: %d".formatted(threadPoolCoreSize));
            ConfigValidator.value(threadPoolMaxSize, value -> value > 0, () -> "ThreadPoolMaxSize is <= 0: %d".formatted(threadPoolMaxSize));

            if (threadPoolCoreSize > threadPoolMaxSize) {
                throw new IllegalArgumentException("ThreadPoolCoreSize bigger than ThreadPoolMaxSize: %d > %d".formatted(threadPoolCoreSize, threadPoolMaxSize));
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

        public Builder threadNamePattern(final String threadNamePattern) {
            this.threadNamePattern = threadNamePattern;

            return this;
        }

        public Builder threadPoolCoreSize(final Integer threadPoolCoreSize) {
            if (threadPoolCoreSize == null) {
                return this;
            }

            this.threadPoolCoreSize = threadPoolCoreSize;

            return this;
        }

        public Builder threadPoolMaxSize(final Integer threadPoolMaxSize) {
            if (threadPoolMaxSize == null) {
                return this;
            }

            this.threadPoolMaxSize = threadPoolMaxSize;

            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final int port;
    private final String threadNamePattern;
    private final int threadPoolCoreSize;
    private final int threadPoolMaxSize;

    private ServerConfig(final Builder builder) {
        super();

        port = builder.port;
        threadNamePattern = builder.threadNamePattern;
        threadPoolCoreSize = builder.threadPoolCoreSize;
        threadPoolMaxSize = builder.threadPoolMaxSize;
    }

    public int getPort() {
        return port;
    }

    public String getThreadNamePattern() {
        return threadNamePattern;
    }

    public int getThreadPoolCoreSize() {
        return threadPoolCoreSize;
    }

    public int getThreadPoolMaxSize() {
        return threadPoolMaxSize;
    }
}
