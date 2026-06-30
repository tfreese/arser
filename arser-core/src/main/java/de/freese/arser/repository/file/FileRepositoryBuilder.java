package de.freese.arser.repository.file;

import java.util.Objects;

import de.freese.arser.connector.decorator.LoggingConnector;
import de.freese.arser.connector.file.FileConnector;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.repository.AbstractRepositoryBuilder;

/**
 * @author Thomas Freese
 */
public final class FileRepositoryBuilder extends AbstractRepositoryBuilder<FileRepositoryBuilder, FileRepository> {
    private boolean readOnly = true;

    FileRepositoryBuilder() {
        super();
    }

    @Override
    public FileRepository build() throws Exception {
        Objects.requireNonNull(getUri(), "URI required");
        Objects.requireNonNull(getName(), "name required");

        Connector connector = new FileConnector();

        if (isLogging()) {
            connector = new LoggingConnector(connector);
        }

        return new FileRepository(getUri(), getName(), connector, readOnly);
    }

    public FileRepositoryBuilder readOnly(final boolean readOnly) {
        this.readOnly = readOnly;

        return self();
    }

    @Override
    protected FileRepositoryBuilder self() {
        return this;
    }
}
