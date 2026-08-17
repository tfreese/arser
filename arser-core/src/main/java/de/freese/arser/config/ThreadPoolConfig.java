package de.freese.arser.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * @author Thomas Freese
 * @since 11.05.2025
 */
@JsonDeserialize(builder = ThreadPoolConfig.ThreadPoolConfigBuilder.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(value = {"namePattern", "coreSize", "maxSize"})
public final class ThreadPoolConfig {

    @JsonPOJOBuilder(withPrefix = "")
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.NONE)
    public static final class ThreadPoolConfigBuilder {
        private int coreSize;
        private int maxSize;
        private String namePattern;

        public ThreadPoolConfig build() {
            ConfigValidator.string(namePattern, () -> "namePattern required: '%s'".formatted(namePattern));
            ConfigValidator.value(coreSize, value -> value > 0, () -> "coreSize is <= 0: %d".formatted(coreSize));
            ConfigValidator.value(maxSize, value -> value > 0, () -> "maxSize is <= 0: %d".formatted(maxSize));

            if (coreSize > maxSize) {
                throw new IllegalArgumentException("coreSize bigger than maxSize: %d > %d".formatted(coreSize, maxSize));
            }

            return new ThreadPoolConfig(this);
        }

        public ThreadPoolConfigBuilder coreSize(final int coreSize) {
            this.coreSize = coreSize;

            return this;
        }

        public ThreadPoolConfigBuilder maxSize(final int maxSize) {
            this.maxSize = maxSize;

            return this;
        }

        public ThreadPoolConfigBuilder namePattern(final String namePattern) {
            this.namePattern = namePattern;

            return this;
        }
    }

    public static ThreadPoolConfigBuilder builder() {
        return new ThreadPoolConfigBuilder();
    }

    public static ThreadPoolConfigBuilder builderClientDefault() {
        return new ThreadPoolConfigBuilder()
                .namePattern("http-client-%d")
                .coreSize(2)
                .maxSize(6)
                ;
    }

    public static ThreadPoolConfigBuilder builderServerDefault() {
        return new ThreadPoolConfigBuilder()
                .namePattern("http-server-%d")
                .coreSize(2)
                .maxSize(6)
                ;
    }

    private final int coreSize;
    private final int maxSize;
    private final String namePattern;

    private ThreadPoolConfig(final ThreadPoolConfigBuilder builder) {
        super();

        namePattern = builder.namePattern;
        coreSize = builder.coreSize;
        maxSize = builder.maxSize;
    }

    public int coreSize() {
        return coreSize;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ThreadPoolConfig that = (ThreadPoolConfig) o;

        return coreSize == that.coreSize && maxSize == that.maxSize && Objects.equals(namePattern, that.namePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coreSize, maxSize, namePattern);
    }

    public int maxSize() {
        return maxSize;
    }

    public ThreadPoolConfigBuilder mutate() {
        return new ThreadPoolConfigBuilder()
                .namePattern(namePattern())
                .coreSize(coreSize())
                .maxSize(maxSize())
                ;
    }

    public String namePattern() {
        return namePattern;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + " ["
                + "namePattern=" + namePattern
                + ", coreSize=" + coreSize
                + ", maxSize=" + maxSize
                + ']';
    }
}
