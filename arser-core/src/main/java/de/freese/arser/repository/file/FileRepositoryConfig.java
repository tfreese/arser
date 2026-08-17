package de.freese.arser.repository.file;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import de.freese.arser.repository.AbstractRepositoryConfig;

/**
 * @author Thomas Freese
 */
@JsonDeserialize(builder = FileRepositoryConfig.FileRepositoryConfigBuilder.class)
@JsonTypeName("fileRepositoryConfig")
@JsonPropertyOrder(value = {"type", "name", "uri", "logging", "readOnly"})
// @JsonPropertyOrder(alphabetic = true)
public final class FileRepositoryConfig extends AbstractRepositoryConfig {

    @JsonPOJOBuilder(withPrefix = "")
    public static final class FileRepositoryConfigBuilder extends AbstractRepositoryConfig.Builder<FileRepositoryConfigBuilder> {
        private boolean readOnly = true;

        @Override
        public FileRepositoryConfig build() {
            return new FileRepositoryConfig(this);
        }

        public FileRepositoryConfigBuilder readOnly(final boolean readOnly) {
            this.readOnly = readOnly;

            return this;
        }
    }

    public static FileRepositoryConfigBuilder builder() {
        return new FileRepositoryConfigBuilder();
    }

    private final boolean readOnly;

    // @JsonCreator
    // FileRepositoryConfig(@JsonProperty("name") final String name) {
    private FileRepositoryConfig(final FileRepositoryConfigBuilder builder) {
        super(builder);

        readOnly = builder.readOnly;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        final FileRepositoryConfig that = (FileRepositoryConfig) o;

        return readOnly == that.readOnly;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), readOnly);
    }

    @Override
    public FileRepositoryConfigBuilder mutate() {
        return new FileRepositoryConfigBuilder()
                .name(name())
                .uri(uri())
                .logging(logging())
                .readOnly(readOnly());
    }

    public boolean readOnly() {
        return readOnly;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "["
                + "name=" + name()
                + "uri=" + uri()
                + "logging=" + logging()
                + "readOnly=" + readOnly()
                + ']';
    }
}
