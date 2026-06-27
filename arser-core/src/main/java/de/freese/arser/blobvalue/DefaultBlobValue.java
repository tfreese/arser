package de.freese.arser.blobvalue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Keeps Data in Memory.<br/>
 * If MemoryThreshold reached, a temp.-File ist used.
 *
 * @author Thomas Freese
 */
public final class DefaultBlobValue implements BlobValue {
    // 10 MB
    private static final int DEFAULT_MEMORY_THRESHOLD = 10 * 1024 * 1024;

    public static BlobValue of(final byte[] data) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return of(inputStream, DEFAULT_MEMORY_THRESHOLD);
        }
    }

    public static BlobValue of(final byte[] data, final int memoryThreshold) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return of(inputStream, memoryThreshold);
        }
    }

    public static BlobValue of(final InputStream inputStream) throws IOException {
        return of(inputStream, DEFAULT_MEMORY_THRESHOLD);
    }

    public static BlobValue of(final InputStream inputStream, final int memoryThreshold) throws IOException {
        final DefaultBlobValue blobValue = new DefaultBlobValue(memoryThreshold);
        blobValue.readFrom(inputStream);

        return blobValue;
    }

    private final int memoryThreshold;

    private OutputStream currentOutputStream;

    private boolean isInMemory = true;
    private ByteArrayOutputStream memoryBuffer;
    private Path tempFile;

    private DefaultBlobValue(final int memoryThreshold) {
        super();

        this.memoryThreshold = memoryThreshold;
        this.memoryBuffer = new ByteArrayOutputStream();
        this.currentOutputStream = memoryBuffer;
    }

    @Override
    public void close() throws IOException {
        if (currentOutputStream != null) {
            currentOutputStream.close();
        }

        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public InputStream createInputStream() throws Exception {
        if (isInMemory) {
            return new ByteArrayInputStream(memoryBuffer.toByteArray());
        } else {
            return new BufferedInputStream(Files.newInputStream(tempFile));
        }
    }

    @Override
    public long getContentLength() throws Exception {
        if (isInMemory) {
            return memoryBuffer.size();
        } else {
            return Files.size(tempFile);
        }
    }

    void readFrom(final InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[8192];
        int bytesRead;
        long totalBytes = 0L;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            totalBytes += bytesRead;

            // Strategie-Wechsel: Wenn Limit überschritten, auf Festplatte auslagern.
            if (isInMemory && totalBytes > memoryThreshold) {
                switchToDisk();
            }

            currentOutputStream.write(buffer, 0, bytesRead);
        }

        currentOutputStream.flush();
    }

    private void switchToDisk() throws IOException {
        isInMemory = false;

        tempFile = Files.createTempFile("blobValue", ".tmp");
        final OutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile.toFile()));

        // Bereits gelesene Bytes aus dem RAM in die Datei schreiben.
        memoryBuffer.writeTo(fileOutputStream);
        memoryBuffer = null; // RAM freigeben

        currentOutputStream = fileOutputStream;
    }
}
