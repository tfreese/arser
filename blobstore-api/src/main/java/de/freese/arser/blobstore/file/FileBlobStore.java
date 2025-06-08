// Created: 18.09.2019
package de.freese.arser.blobstore.file;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import de.freese.arser.blobstore.api.AbstractBlobStore;
import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.ThrowingConsumer;

/**
 * @author Thomas Freese
 */
public class FileBlobStore extends AbstractBlobStore {

    private final URI uri;

    public FileBlobStore(final URI uri) {
        super();

        this.uri = Objects.requireNonNull(uri, "URI required");
    }

    @Override
    public Blob create(final BlobId id, final ThrowingConsumer<OutputStream, Exception> consumer) throws Exception {
        final Path path = toContentPath(id);

        Files.createDirectories(path.getParent());

        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            consumer.accept(outputStream);

            outputStream.flush();
        }

        return get(id);
    }

    @Override
    public Blob create(final BlobId id, final InputStream inputStream) throws Exception {
        final Path path = toContentPath(id);

        Files.createDirectories(path.getParent());

        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

        return get(id);
    }

    @Override
    public void delete(final BlobId id) throws Exception {
        final Path path = toContentPath(id);

        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    @Override
    public boolean exists(final BlobId id) throws Exception {
        final Path path = toContentPath(id);

        return Files.exists(path);
    }

    @Override
    public Blob get(final BlobId id) throws Exception {
        return new FileBlob(id, toContentPath(id));
    }

    @Override
    public URI getUri() {
        return uri;
    }

    Path toContentPath(final BlobId id) {
        String uriString = id.getUri().getPath();

        uriString = uriString.replace(':', '/');
        uriString = uriString.replace('?', '/');
        uriString = uriString.replace('&', '/');
        uriString = uriString.replace(' ', '_');
        uriString = uriString.replace("%20", "_");

        while (uriString.contains("//")) {
            uriString = uriString.replace("//", "/");
        }

        if (uriString.startsWith("/")) {
            uriString = uriString.substring(1);
        }

        return Paths.get(getUri()).resolve(uriString);

        //        final byte[] uriBytes = uriString.getBytes(StandardCharsets.UTF_8);
        //        final byte[] digest = getMessageDigest().digest(uriBytes);
        //        final String hex = HexFormat.of().withUpperCase().formatHex(uriBytes);
        //
        //        Path path = basePath;
        //
        //        // Build Structure in the Cache-Directory.
        //        for (int i = 0; i < 3; i++)
        //        {
        //            path = path.resolve(hex.substring(i * 2, (i * 2) + 2));
        //        }
        //
        //        return basePath.resolve(hex);
    }
}
