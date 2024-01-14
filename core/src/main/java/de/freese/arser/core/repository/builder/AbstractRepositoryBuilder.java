// Created: 14.01.24
package de.freese.arser.core.repository.builder;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.core.repository.configurer.RepositoryConfigurer;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRepositoryBuilder implements RepositoryConfigurer {
    private static final Pattern PATTERN_NAME = Pattern.compile("([a-z0-9\\-_])+");

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String name;
    private URI uri;

    public void setName(final String name) {
        this.name = name;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected String getName() {
        return name;
    }

    protected URI getUri() {
        return uri;
    }

    protected void validateName() {
        Objects.requireNonNull(name, "name required");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name is empty");
        }

        if (!PATTERN_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("name must match the pattern: " + PATTERN_NAME);
        }
    }

    protected void validateUri() {
        Objects.requireNonNull(uri, "uri required");
    }
}
