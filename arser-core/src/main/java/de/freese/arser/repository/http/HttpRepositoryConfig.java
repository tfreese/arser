package de.freese.arser.repository.http;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import de.freese.arser.repository.AbstractRepositoryConfig;

/**
 * @author Thomas Freese
 */
@JsonDeserialize(builder = HttpRepositoryConfig.HttpRepositoryConfigBuilder.class)
@JsonTypeName("httpRepositoryConfig")
@JsonPropertyOrder(value = {"type", "name", "uri", "logging", "cachingPath", "connectTimeout", "maxRetries", "retryInterval"})
public final class HttpRepositoryConfig extends AbstractRepositoryConfig {
    @JsonPOJOBuilder(withPrefix = "")
    public static final class HttpRepositoryConfigBuilder extends AbstractRepositoryConfig.Builder<HttpRepositoryConfigBuilder> {
        // private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30L);
        // private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30L);

        private Path cachingPath;
        private Duration connectTimeout = Duration.ofSeconds(30L);
        private int maxRetries = 3;
        private Duration retryInterval = Duration.ofSeconds(3L);

        @Override
        public HttpRepositoryConfig build() {
            if (maxRetries <= 0) {
                throw new IllegalArgumentException("maxRetries must be greater than 0: " + maxRetries);
            }

            Objects.requireNonNull(connectTimeout, "connectTimeout required");

            if (connectTimeout.isNegative()) {
                throw new IllegalArgumentException("connectTimeout cannot be negative: " + connectTimeout);
            }

            if (connectTimeout.isZero()) {
                throw new IllegalArgumentException("connectTimeout cannot be zero");
            }

            Objects.requireNonNull(retryInterval, "retryInterval required");

            if (retryInterval.isNegative()) {
                throw new IllegalArgumentException("retryInterval cannot be negative: " + retryInterval);
            }

            if (retryInterval.isZero()) {
                throw new IllegalArgumentException("retryInterval cannot be zero:");
            }

            return new HttpRepositoryConfig(this);
        }

        public HttpRepositoryConfigBuilder cachingPath(final Path cachingPath) {
            this.cachingPath = cachingPath;

            return this;
        }

        public HttpRepositoryConfigBuilder connectTimeout(final Duration connectTimeout) {
            this.connectTimeout = connectTimeout;

            return this;
        }

        public HttpRepositoryConfigBuilder maxRetries(final int maxRetries) {
            this.maxRetries = maxRetries;

            return this;
        }

        public HttpRepositoryConfigBuilder retryInterval(final Duration retryInterval) {
            this.retryInterval = retryInterval;

            return this;
        }

        public HttpRepositoryConfigBuilder withRetrying(final int maxRetries, final Duration retryInterval) {
            this.maxRetries = maxRetries;
            this.retryInterval = retryInterval;

            return this;
        }

        // public HttpRepositoryConfigBuilder sslContext(final SSLContext sslContext) {
        //     this.sslContext = sslContext;
        //
        //     return this;
        // }
    }

    public static HttpRepositoryConfigBuilder builder() {
        return new HttpRepositoryConfigBuilder();
    }

    private final Path cachingPath;
    private final Duration connectTimeout;
    private final int maxRetries;
    private final Duration retryInterval;

    private HttpRepositoryConfig(final HttpRepositoryConfigBuilder builder) {
        super(builder);

        this.cachingPath = builder.cachingPath;
        this.connectTimeout = builder.connectTimeout;
        this.maxRetries = builder.maxRetries;
        this.retryInterval = builder.retryInterval;
    }

    public Path cachingPath() {
        return cachingPath;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        final HttpRepositoryConfig that = (HttpRepositoryConfig) o;

        return maxRetries == that.maxRetries
                && Objects.equals(cachingPath, that.cachingPath)
                && Objects.equals(connectTimeout, that.connectTimeout)
                && Objects.equals(retryInterval, that.retryInterval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cachingPath, connectTimeout, maxRetries, retryInterval);
    }

    public int maxRetries() {
        return maxRetries;
    }

    @Override
    public HttpRepositoryConfigBuilder mutate() {
        return new HttpRepositoryConfigBuilder()
                .name(name())
                .uri(uri())
                .cachingPath(cachingPath())
                .connectTimeout(connectTimeout())
                .maxRetries(maxRetries())
                .retryInterval(retryInterval());
    }

    public Duration retryInterval() {
        return retryInterval;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "["
                + "name=" + name()
                + ", uri=" + uri()
                + ", logging=" + logging()
                + ", cachingPath=" + cachingPath()
                + ", connectTimeout=" + connectTimeout()
                + ", maxRetries=" + maxRetries()
                + ", retryInterval=" + retryInterval()
                + ']';
    }
}
