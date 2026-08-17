package de.freese.arser.repository;

import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.freese.arser.repository.file.FileRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.virtual.VirtualRepositoryConfig;

/**
 * @author Thomas Freese
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
// 1. Jackson mitteilen, wie die Unterklassen im JSON erkannt werden.
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,   // Diskriminator als normales Property
        property = "type"                     // Name des Diskriminator-Felds
)
// 2. Alle bekannten Unterklassen registrieren
@JsonSubTypes({
        @JsonSubTypes.Type(value = FileRepositoryConfig.class, name = "fileRepositoryConfig"),
        @JsonSubTypes.Type(value = HttpRepositoryConfig.class, name = "httpRepositoryConfig"),
        @JsonSubTypes.Type(value = VirtualRepositoryConfig.class, name = "virtualRepositoryConfig")
})
@SuppressWarnings({"java:S1452"})
public abstract class AbstractRepositoryConfig {
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.NONE)
    public abstract static class Builder<B extends Builder<B>> {
        private boolean logging;
        private String name;
        private URI uri;

        // InjectableValues inject = new InjectableValues.Std()
        //         .addValue("lifeCycleRegistry", lifeCycleRegistry);
        // @JacksonInject("lifeCycleRegistry")
        // private LifeCycleRegistry lifeCycleRegistry;
        // public Repository build(@JacksonInject("lifeCycleRegistry") final LifeCycleRegistry lifeCycleRegistry) throws Exception
        public abstract AbstractRepositoryConfig build();

        public B logging(final boolean logging) {
            this.logging = logging;

            return self();
        }

        public B name(final String name) {
            this.name = name;

            return self();
        }

        public B uri(final URI uri) {
            this.uri = uri;

            return self();
        }

        public B withLogging() {
            return logging(true);
        }

        /**
         * Gibt 'this' als den korrekten Unterklassen-Typ zurück.
         */
        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }
    }

    private final boolean logging;
    private final String name;
    private final URI uri;

    protected AbstractRepositoryConfig(final Builder<?> builder) {
        super();

        this.name = Objects.requireNonNull(builder.name, "name required");
        this.uri = Objects.requireNonNull(builder.uri, "uri required").normalize();
        this.logging = builder.logging;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AbstractRepositoryConfig that = (AbstractRepositoryConfig) o;

        return logging == that.logging && Objects.equals(name, that.name) && Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logging, name, uri);
    }

    public boolean logging() {
        return logging;
    }

    public abstract Builder<?> mutate();

    public String name() {
        return name;
    }

    public URI uri() {
        return uri;
    }
}
