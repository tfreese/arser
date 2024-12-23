// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public final class RemoteRepositoryConfig extends AbstractRepositoryConfig {
    public static final class Builder {
        private String contextRoot;
        private StoreConfig storeConfig;
        private URI uri;

        private Builder() {
            super();
        }

        public RemoteRepositoryConfig build() {
            ConfigValidator.contextRoot(contextRoot);
            ConfigValidator.uri(uri);
            // ConfigValidator.value(storeConfig, Objects::nonNull, () -> "storeConfig required");

            return new RemoteRepositoryConfig(this);
        }

        public Builder contextRoot(final String contextRoot) {
            this.contextRoot = contextRoot;

            return this;
        }

        public Builder storeConfig(final StoreConfig storeConfig) {
            this.storeConfig = storeConfig;

            return this;
        }

        public Builder uri(final URI uri) {
            this.uri = uri;

            return this;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    private final StoreConfig storeConfig;

    private RemoteRepositoryConfig(final Builder builder) {
        super(builder.contextRoot, builder.uri);

        storeConfig = builder.storeConfig;
    }

    public StoreConfig getStoreConfig() {
        return storeConfig;
    }
}
