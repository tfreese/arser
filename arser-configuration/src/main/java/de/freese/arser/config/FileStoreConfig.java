// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public final class FileStoreConfig {
    public static final class Builder {
        private URI uri;

        private Builder() {
            super();
        }

        public FileStoreConfig build() {
            ConfigValidator.uri(uri);

            return new FileStoreConfig(this);
        }

        public Builder uri(final URI uri) {
            this.uri = uri;

            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final URI uri;

    private FileStoreConfig(final Builder builder) {
        super();

        uri = builder.uri;
    }

    public URI getUri() {
        return uri;
    }
}
