package de.freese.arser.blobstore.file;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import de.freese.arser.blobstore.api.AbstractBlob;
import de.freese.arser.blobstore.api.BlobId;

/**
 * @author Thomas Freese
 */
final class FileBlob extends AbstractBlob {

    private final Path absolutePath;
    private final boolean exist;

    FileBlob(final BlobId id, final Path absolutePath) {
        super(id);

        this.absolutePath = absolutePath;
        this.exist = Files.exists(absolutePath);
    }

    @Override
    public InputStream getInputStream() throws Exception {
        if (!exist) {
            return InputStream.nullInputStream();
        }

        return Files.newInputStream(this.absolutePath);
    }

    @Override
    public long getLength() throws Exception {
        if (!exist) {
            return -1L;
        }

        return Files.size(this.absolutePath);
    }

    @Override
    public String toString() {
        return this.absolutePath.toString();
    }
}
