// Created: 31 Okt. 2024
package de.freese.arser.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Freese
 */
public final class VirtualRepositoryConfig extends AbstractRepositoryConfig {
    public static final class Builder {
        // private final List<Repository> repositories = new ArrayList<>();
        private final List<String> repositoryRefs = new ArrayList<>();
        private String contextRoot;

        private Builder() {
            super();
        }

        public VirtualRepositoryConfig build() {
            ConfigValidator.contextRoot(contextRoot);

            // if (!repositories.isEmpty() && !repositoryRefs.isEmpty()) {
            //     throw new IllegalStateException("either repositories OR repositoryRefs can be used");
            // }
            //
            // if (repositories.isEmpty()) {
            //     ConfigValidator.value(repositoryRefs, value -> !value.isEmpty(), () -> "repositoryRefs are empty");
            // }
            //
            // if (repositoryRefs.isEmpty()) {
            //     ConfigValidator.value(repositories, value -> !value.isEmpty(), () -> "repositories are empty");
            // }

            return new VirtualRepositoryConfig(this);
        }

        public Builder contextRoot(final String contextRoot) {
            this.contextRoot = contextRoot;

            return this;
        }

        // public Builder repositories(final List<Repository> repositories) {
        //     if (repositories == null) {
        //         return this;
        //     }
        //
        //     repositories.forEach(repository -> {
        //         if (this.repositories.contains(repository)) {
        //             throw new IllegalArgumentException("repository already exist: " + repository.getContextRoot());
        //         }
        //
        //         this.repositories.add(repository);
        //     });
        //
        //     return this;
        // }

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

    // private final List<Repository> repositories;
    private final List<String> repositoryRefs;

    private VirtualRepositoryConfig(final Builder builder) {
        super(builder.contextRoot, URI.create("virtual"));

        repositoryRefs = List.copyOf(builder.repositoryRefs);
        // repositories = List.copyOf(builder.repositories);
    }

    // public List<Repository> getRepositories() {
    //     return repositories;
    // }

    public List<String> getRepositoryRefs() {
        return repositoryRefs;
    }
}
