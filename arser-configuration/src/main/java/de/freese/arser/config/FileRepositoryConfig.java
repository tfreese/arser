// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public final class FileRepositoryConfig extends AbstractRepositoryConfig {
    public static final class Builder {
        private String contextRoot;
        private URI uri;
        private boolean writeable;

        private Builder() {
            super();
        }

        public FileRepositoryConfig build() {
            ConfigValidator.contextRoot(contextRoot);
            ConfigValidator.uri(uri);

            return new FileRepositoryConfig(this);
        }

        public Builder contextRoot(final String contextRoot) {
            this.contextRoot = contextRoot;

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

    private FileRepositoryConfig(final Builder builder) {
        super(builder.contextRoot, builder.uri);

        writeable = builder.writeable;
    }

    public boolean isWriteable() {
        return writeable;
    }
}
