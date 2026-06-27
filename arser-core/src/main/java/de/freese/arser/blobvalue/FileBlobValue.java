package de.freese.arser.blobvalue;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Thomas Freese
 */
public final class FileBlobValue implements BlobValue {
    private final Path path;

    public FileBlobValue(final Path path) {
        super();

        this.path = Objects.requireNonNull(path, "path required");
    }

    @Override
    public void close() {
        // Empty
    }

    @Override
    public InputStream createInputStream() throws Exception {
        return new BufferedInputStream(Files.newInputStream(path));
    }

    @Override
    public long getContentLength() throws Exception {
        return Files.size(path);
    }
}
