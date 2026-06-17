package de.freese.arser.core.model;

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
 * @author Thomas Freese
 */
public final class BlobValue implements AutoCloseable {
    // 10 MB
    private static final int DEFAULT_MEMORY_THRESHOLD = 10 * 1024 * 1024;

    public static BlobValue of(final InputStream inputStream) throws IOException {
        return of(DEFAULT_MEMORY_THRESHOLD, inputStream);
    }

    public static BlobValue of(final int memoryThreshold, final InputStream inputStream) throws IOException {
        final BlobValue blobValue = new BlobValue(memoryThreshold);
        blobValue.readFrom(inputStream);

        return blobValue;
    }

    private final int memoryThreshold;

    private OutputStream currentOutputStream;
    private boolean isInMemory = true;
    private ByteArrayOutputStream memoryBuffer;
    private Path tempFile;

    private BlobValue(final int memoryThreshold) {
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

    public InputStream getInputStream() throws Exception {
        if (isInMemory) {
            return new ByteArrayInputStream(memoryBuffer.toByteArray());
        } else {
            return new BufferedInputStream(Files.newInputStream(tempFile));
        }
    }

    private void readFrom(final InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[8192];
        int bytesRead;
        long totalBytes = 0L;

        try (inputStream) {
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
    }

    private void switchToDisk() throws IOException {
        isInMemory = false;
        
        tempFile = Files.createTempFile("dataContainer_", ".tmp");
        final OutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile.toFile()));

        // Bereits gelesene Bytes aus dem RAM in die Datei schreiben.
        memoryBuffer.writeTo(fileOutputStream);
        memoryBuffer = null; // RAM freigeben

        currentOutputStream = fileOutputStream;
    }
}
