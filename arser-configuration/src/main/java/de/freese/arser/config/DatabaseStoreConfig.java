// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public final class DatabaseStoreConfig {
    public static final class Builder {
        private String driverClassName;
        private String password;
        private int poolCoreSize;
        private int poolMaxSize;
        private String poolName;
        private URI uri;
        private String user;

        private Builder() {
            super();
        }

        public DatabaseStoreConfig build() {
            ConfigValidator.uri(uri);
            ConfigValidator.string(driverClassName, () -> "driverClassName required: '%s'".formatted(driverClassName));
            ConfigValidator.string(user, () -> "user required: '%s'".formatted(user));
            ConfigValidator.string(password, () -> "password required: '%s'".formatted(password));
            ConfigValidator.value(poolCoreSize, value -> value > 0, () -> "PoolCoreSize is <= 0: %d".formatted(poolCoreSize));
            ConfigValidator.value(poolMaxSize, value -> value > 0, () -> "PoolMaxSize is <= 0: %d".formatted(poolMaxSize));

            if (poolCoreSize > poolMaxSize) {
                throw new IllegalArgumentException("PoolCoreSize bigger than PoolMaxSize: %d > %d".formatted(poolCoreSize, poolMaxSize));
            }

            return new DatabaseStoreConfig(this);
        }

        public Builder driverClassName(final String driverClassName) {
            this.driverClassName = driverClassName;

            return this;
        }

        public Builder password(final String password) {
            this.password = password;

            return this;
        }

        public Builder poolCoreSize(final Integer poolCoreSize) {
            if (poolCoreSize == null) {
                return this;
            }

            this.poolCoreSize = poolCoreSize;

            return this;
        }

        public Builder poolMaxSize(final Integer poolMaxSize) {
            if (poolMaxSize == null) {
                return this;
            }

            this.poolMaxSize = poolMaxSize;

            return this;
        }

        public Builder poolName(final String poolName) {
            this.poolName = poolName;

            return this;
        }

        public Builder uri(final URI uri) {
            this.uri = uri;

            return this;
        }

        public Builder user(final String user) {
            this.user = user;

            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String driverClassName;
    private final String password;
    private final Integer poolCoreSize;
    private final Integer poolMaxSize;
    private final String poolName;
    private final URI uri;
    private final String user;

    private DatabaseStoreConfig(final Builder builder) {
        super();

        uri = builder.uri;
        driverClassName = builder.driverClassName;
        user = builder.user;
        password = builder.password;
        poolCoreSize = builder.poolCoreSize;
        poolMaxSize = builder.poolMaxSize;
        poolName = builder.poolName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getPassword() {
        return password;
    }

    public Integer getPoolCoreSize() {
        return poolCoreSize;
    }

    public Integer getPoolMaxSize() {
        return poolMaxSize;
    }

    public String getPoolName() {
        return poolName;
    }

    public URI getUri() {
        return uri;
    }

    public String getUser() {
        return user;
    }
}
