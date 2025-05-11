// Created: 11 Mai 2025
package de.freese.arser.config;

/**
 * @author Thomas Freese
 */
public final class ThreadPoolConfig {
    public static final class Builder {
        private int coreSize;
        private int maxSize;
        private String namePattern;

        private Builder() {
            super();
        }

        public ThreadPoolConfig build() {
            ConfigValidator.string(namePattern, () -> "namePattern required: '%s'".formatted(namePattern));
            ConfigValidator.value(coreSize, value -> value > 0, () -> "coreSize is <= 0: %d".formatted(coreSize));
            ConfigValidator.value(maxSize, value -> value > 0, () -> "maxSize is <= 0: %d".formatted(maxSize));

            if (coreSize > maxSize) {
                throw new IllegalArgumentException("coreSize bigger than maxSize: %d > %d".formatted(coreSize, maxSize));
            }

            return new ThreadPoolConfig(this);
        }

        public ThreadPoolConfig.Builder coreSize(final Integer coreSize) {
            if (coreSize == null) {
                return this;
            }

            this.coreSize = coreSize;

            return this;
        }

        public ThreadPoolConfig.Builder maxSize(final Integer maxSize) {
            if (maxSize == null) {
                return this;
            }

            this.maxSize = maxSize;

            return this;
        }

        public ThreadPoolConfig.Builder namePattern(final String namePattern) {
            this.namePattern = namePattern;

            return this;
        }
    }

    public static ThreadPoolConfig.Builder builder() {
        return new ThreadPoolConfig.Builder();
    }

    public static ThreadPoolConfig.Builder builderClientDefault() {
        return new ThreadPoolConfig.Builder()
                .namePattern("http-client-%d")
                .coreSize(2)
                .maxSize(6)
                ;
    }

    public static ThreadPoolConfig.Builder builderServerDefault() {
        return new ThreadPoolConfig.Builder()
                .namePattern("http-server-%d")
                .coreSize(2)
                .maxSize(6)
                ;
    }

    private final int coreSize;
    private final int maxSize;
    private final String namePattern;

    private ThreadPoolConfig(final Builder builder) {
        super();

        namePattern = builder.namePattern;
        coreSize = builder.coreSize;
        maxSize = builder.maxSize;
    }

    public int getCoreSize() {
        return coreSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public String getNamePattern() {
        return namePattern;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [");
        sb.append("namePattern='").append(namePattern).append('\'');
        sb.append(", coreSize=").append(coreSize);
        sb.append(", maxSize=").append(maxSize);
        sb.append(']');

        return sb.toString();
    }
}
