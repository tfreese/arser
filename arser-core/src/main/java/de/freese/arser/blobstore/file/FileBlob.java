package de.freese.arser.blobstore.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import de.freese.arser.blobstore.api.AbstractBlob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.blobvalue.EmptyBlobValue;
import de.freese.arser.blobvalue.FileBlobValue;

/**
 * @author Thomas Freese
 */
final class FileBlob extends AbstractBlob {

    private final Path absolutePath;
    private final BlobValue blobValue;
    private final boolean exist;

    FileBlob(final BlobId id, final Path absolutePath) {
        super(id);

        this.absolutePath = Objects.requireNonNull(absolutePath, "absolutePath required");

        exist = Files.exists(absolutePath);

        if (exist) {
            blobValue = new FileBlobValue(absolutePath);
        } else {
            blobValue = new EmptyBlobValue();
        }
    }

    @Override
    public BlobValue getBlobValue() {
        return blobValue;
    }

    @Override
    public String toString() {
        return absolutePath.toString();
    }
}
