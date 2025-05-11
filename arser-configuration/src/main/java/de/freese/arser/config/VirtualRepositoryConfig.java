// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Freese
 */
public final class VirtualRepositoryConfig {
    public static final class Builder {
        private final List<String> repositoryRefs = new ArrayList<>();
        private String contextRoot;

        private Builder() {
            super();
        }

        public VirtualRepositoryConfig build() {
            ConfigValidator.contextRoot(contextRoot);

            return new VirtualRepositoryConfig(this);
        }

        public Builder contextRoot(final String contextRoot) {
            this.contextRoot = contextRoot;

            return this;
        }

        public Builder repositoryRefs(final List<String> repositoryRefs) {
            if (repositoryRefs == null) {
                return this;
            }

            repositoryRefs.forEach(ref -> {
                if (this.repositoryRefs.contains(ref)) {
                    throw new IllegalArgumentException("repository already exist: " + ref);
                }

                this.repositoryRefs.add(ref);
            });

            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String contextRoot;
    private final List<String> repositoryRefs;
    private final URI uri;

    private VirtualRepositoryConfig(final Builder builder) {
        super();

        contextRoot = builder.contextRoot;
        uri = URI.create("virtual");
        repositoryRefs = List.copyOf(builder.repositoryRefs);
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public List<String> getRepositoryRefs() {
        return repositoryRefs;
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
        sb.append(", repositoryRefs=").append(repositoryRefs);
        sb.append(']');

        return sb.toString();
    }
}
