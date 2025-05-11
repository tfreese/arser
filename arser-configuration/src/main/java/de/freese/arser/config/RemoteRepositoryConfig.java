// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public final class RemoteRepositoryConfig {
    public static final class Builder {
        private String contextRoot;
        private URI uri;

        private Builder() {
            super();
        }

        public RemoteRepositoryConfig build() {
            ConfigValidator.contextRoot(contextRoot);
            ConfigValidator.uri(uri);

            return new RemoteRepositoryConfig(this);
        }

        public Builder contextRoot(final String contextRoot) {
            this.contextRoot = contextRoot;

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

    private final String contextRoot;
    private final URI uri;

    private RemoteRepositoryConfig(final Builder builder) {
        super();

        contextRoot = builder.contextRoot;
        uri = builder.uri;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [");
        sb.append("contextRoot=").append(getContextRoot());
        sb.append(", uri=").append(getUri());
        sb.append(']');

        return sb.toString();
    }
}
