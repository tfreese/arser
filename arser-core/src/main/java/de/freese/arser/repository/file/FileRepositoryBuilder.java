package de.freese.arser.repository.file;

import java.net.URI;
import java.util.Objects;

import de.freese.arser.connector.file.FileConnector;
import de.freese.arser.utils.AbstractBuilder;

/**
 * @author Thomas Freese
 */
public final class FileRepositoryBuilder extends AbstractBuilder<FileRepositoryBuilder, FileRepository> {
    private FileConnector connector;
    private String name;
    private boolean readOnly = true;
    private URI uri;

    FileRepositoryBuilder() {
        super();
    }

    @Override
    public FileRepository build() throws Exception {
        Objects.requireNonNull(uri, "URI required");
        Objects.requireNonNull(name, "name required");
        Objects.requireNonNull(connector, "connector required");

        return new FileRepository(uri, name, connector, readOnly);
    }

    public FileRepositoryBuilder connector(final FileConnector connector) {
        this.connector = connector;

        return self();
    }

    public FileRepositoryBuilder name(final String name) {
        this.name = name;

        return self();
    }

    public FileRepositoryBuilder readOnly(final boolean readOnly) {
        this.readOnly = readOnly;

        return self();
    }

    public FileRepositoryBuilder uri(final URI uri) {
        this.uri = uri;

        return self();
    }

    @Override
    protected FileRepositoryBuilder self() {
        return this;
    }
}
