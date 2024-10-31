// Created: 31 Okt. 2024
package de.freese.arser.core.config;

/**
 * @author Thomas Freese
 */
public final class HttpClientConfig {
    public static final class Builder {
        private String threadNamePattern;
        private int threadPoolCoreSize;
        private int threadPoolMaxSize;

        private Builder() {
            super();
        }

        public HttpClientConfig build() {
            ConfigValidator.string(threadNamePattern, () -> "threadNamePattern required: '%s'".formatted(threadNamePattern));
            ConfigValidator.value(threadPoolCoreSize, value -> value > 0, () -> "ThreadPoolCoreSize is <= 0: %d".formatted(threadPoolCoreSize));
            ConfigValidator.value(threadPoolMaxSize, value -> value > 0, () -> "ThreadPoolMaxSize is <= 0: %d".formatted(threadPoolMaxSize));

            if (threadPoolCoreSize > threadPoolMaxSize) {
                throw new IllegalArgumentException("ThreadPoolCoreSize bigger than ThreadPoolMaxSize: %d > %d".formatted(threadPoolCoreSize, threadPoolMaxSize));
            }

            return new HttpClientConfig(this);
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

    private final String threadNamePattern;
    private final int threadPoolCoreSize;
    private final int threadPoolMaxSize;

    private HttpClientConfig(final Builder builder) {
        super();

        threadNamePattern = builder.threadNamePattern;
        threadPoolCoreSize = builder.threadPoolCoreSize;
        threadPoolMaxSize = builder.threadPoolMaxSize;
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
