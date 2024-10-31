// Created: 31 Okt. 2024
package de.freese.arser.core.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public final class LocalRepositoryConfig extends AbstractRepositoryConfig {
    public static final class Builder {
        private String name;
        private URI uri;
        private boolean writeable;

        private Builder() {
            super();
        }

        public LocalRepositoryConfig build() {
            ConfigValidator.name(name);
            ConfigValidator.uri(uri);

            return new LocalRepositoryConfig(this);
        }

        public Builder name(final String name) {
            this.name = name;

            return this;
        }

        public Builder uri(final URI uri) {
            this.uri = uri;

            return this;
        }

        public Builder writeable(final boolean writeable) {
            this.writeable = writeable;

            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final boolean writeable;

    private LocalRepositoryConfig(final Builder builder) {
        super(builder.name, builder.uri);

        writeable = builder.writeable;
    }

    public boolean isWriteable() {
        return writeable;
    }
}
