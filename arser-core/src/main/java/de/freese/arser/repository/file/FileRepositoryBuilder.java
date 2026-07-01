package de.freese.arser.repository.file;

import java.util.Objects;

import de.freese.arser.component.LifeCycleRegistry;
import de.freese.arser.connector.decorator.LoggingConnectorDecorator;
import de.freese.arser.connector.file.FileConnector;
import de.freese.arser.connector.spi.Connector;
import de.freese.arser.repository.AbstractRepositoryBuilder;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.decorator.LoggingRepositoryDecorator;

/**
 * @author Thomas Freese
 */
public final class FileRepositoryBuilder extends AbstractRepositoryBuilder<FileRepositoryBuilder, Repository> {
    private boolean readOnly = true;

    FileRepositoryBuilder() {
        super();
    }

    @Override
    public Repository build(final LifeCycleRegistry lifeCycleRegistry) throws Exception {
        Objects.requireNonNull(getUri(), "URI required");
        Objects.requireNonNull(getName(), "name required");

        Connector connector = new FileConnector();

        if (isLogging()) {
            connector = new LoggingConnectorDecorator(connector);
        }

        lifeCycleRegistry.register(connector);

        Repository repository = new FileRepository(getUri(), getName(), connector, readOnly);

        if (isLogging()) {
            repository = new LoggingRepositoryDecorator(repository);
        }

        lifeCycleRegistry.register(repository);

        return repository;
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
