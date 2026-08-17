package de.freese.arser.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * @author Thomas Freese
 * @since 31.10.2024
 */
@JsonDeserialize(builder = ServerConfig.ServerConfigBuilder.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(value = {"port", "threadPoolConfig"})
public final class ServerConfig {
    @JsonPOJOBuilder(withPrefix = "")
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.NONE)
    public static final class ServerConfigBuilder {
        private int port;
        private ThreadPoolConfig threadPoolConfig;

        public ServerConfig build() {
            ConfigValidator.value(port, value -> value >= 1025 && value <= 65534, () -> "port not in range 1025-65534: %d".formatted(port));

            Objects.requireNonNull(threadPoolConfig, "threadPoolConfig required");

            return new ServerConfig(this);
        }

        public ServerConfigBuilder port(final int port) {
            this.port = port;

            return this;
        }

        public ServerConfigBuilder threadPoolConfig(final ThreadPoolConfig threadPoolConfig) {
            this.threadPoolConfig = threadPoolConfig;

            return this;
        }
    }

    public static ServerConfigBuilder builder() {
        return new ServerConfigBuilder();
    }

    private final int port;
    private final ThreadPoolConfig threadPoolConfig;

    private ServerConfig(final ServerConfigBuilder builder) {
        super();

        port = builder.port;
        threadPoolConfig = builder.threadPoolConfig;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ServerConfig that = (ServerConfig) o;

        return port == that.port && Objects.equals(threadPoolConfig, that.threadPoolConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, threadPoolConfig);
    }

    public ServerConfigBuilder mutate() {
        return new ServerConfigBuilder()
                .port(port())
                .threadPoolConfig(threadPoolConfig())
                ;
    }

    public int port() {
        return port;
    }

    public ThreadPoolConfig threadPoolConfig() {
        return threadPoolConfig;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + " ["
                + "port=" + port
                + ", threadPoolConfig=" + threadPoolConfig
                + ']';
    }
}
