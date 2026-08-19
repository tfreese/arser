package de.freese.arser.repository.virtual;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import de.freese.arser.repository.AbstractRepositoryConfig;

/**
 * @author Thomas Freese
 * @since 04.07.26
 */
@JsonDeserialize(builder = VirtualRepositoryConfig.VirtualRepositoryConfigBuilder.class)
@JsonTypeName("virtualRepositoryConfig")
@JsonPropertyOrder(value = {"type", "name", "uri", "logging", "repositoryRefs"})
public final class VirtualRepositoryConfig extends AbstractRepositoryConfig {
    @JsonPOJOBuilder(withPrefix = "")
    public static final class VirtualRepositoryConfigBuilder extends AbstractRepositoryConfig.Builder<VirtualRepositoryConfigBuilder> {
        private final List<String> repositoryRefs = new ArrayList<>();

        public VirtualRepositoryConfigBuilder addRepositoryRef(final String repositoryName) {
            Objects.requireNonNull(repositoryName, "repositoryName required");

            if (repositoryRefs.contains(repositoryName)) {
                throw new IllegalStateException("RepositoryName already exists: " + repositoryName);
            }

            this.repositoryRefs.add(repositoryName);

            return self();
        }

        @Override
        public VirtualRepositoryConfig build() {
            if (repositoryRefs.isEmpty()) {
                throw new IllegalStateException("No repository references defined");
            }

            return new VirtualRepositoryConfig(this);
        }

        @JsonSetter("repositoryRefs")
        // @JsonSerialize(as = LinkedHashSet.class)
        // @JsonDeserialize(as = LinkedHashSet.class)
        public VirtualRepositoryConfigBuilder repositoryRefs(final List<String> repositoryRefs) {
            Objects.requireNonNull(repositoryRefs, "repositoryRefs required");

            this.repositoryRefs.clear();
            this.repositoryRefs.addAll(repositoryRefs);

            return this;
        }
    }

    public static VirtualRepositoryConfigBuilder builder() {
        return new VirtualRepositoryConfigBuilder();
    }

    private final List<String> repositoryRefs;

    private VirtualRepositoryConfig(final VirtualRepositoryConfigBuilder builder) {
        super(builder);

        repositoryRefs = List.copyOf(builder.repositoryRefs);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        final VirtualRepositoryConfig that = (VirtualRepositoryConfig) o;

        return Objects.equals(repositoryRefs, that.repositoryRefs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), repositoryRefs);
    }

    @Override
    public VirtualRepositoryConfigBuilder mutate() {
        return new VirtualRepositoryConfigBuilder()
                .name(name())
                .uri(uri())
                .logging(logging())
                .repositoryRefs(repositoryRefs());
    }

    @JacksonXmlElementWrapper(localName = "repositoryRefs")
    @JacksonXmlProperty(localName = "repositoryRef")
    public List<String> repositoryRefs() {
        return repositoryRefs;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "["
                + "name=" + name()
                + ", uri=" + uri()
                + ", logging=" + logging()
                + ", repositoryRefs=" + repositoryRefs()
                + ']';
    }
}
