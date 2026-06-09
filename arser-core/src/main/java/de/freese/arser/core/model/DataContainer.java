package de.freese.arser.core.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Thomas Freese
 */
public class DataContainer implements Closeable {
    // 10 MB
    private static final int MEMORY_THRESHOLD = 10 * 1024 * 1024;

    private final int threshold;
    private OutputStream currentOutputStream;
    private boolean isInMemory = true;
    private ByteArrayOutputStream memoryBuffer;
    private Path tempFile;

    public DataContainer(final int threshold) {
        super();

        this.threshold = threshold;
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
            return Files.newInputStream(tempFile);
        }
    }

    public void readFrom(final InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[8192];
        int bytesRead;
        long totalBytes = 0;

        try (inputStream) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;

                // Strategie-Wechsel: Wenn Limit überschritten, auf Festplatte auslagern.
                if (isInMemory && totalBytes > threshold) {
                    switchToDisk();
                }

                currentOutputStream.write(buffer, 0, bytesRead);
            }

            currentOutputStream.flush();
        }
    }

    private void switchToDisk() throws IOException {
        isInMemory = false;
        tempFile = Files.createTempFile("hybrid_upload_", ".tmp");
        final FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile());

        // Bereits gelesene Bytes aus dem RAM in die Datei schreiben.
        memoryBuffer.writeTo(fileOutputStream);
        memoryBuffer = null; // RAM freigeben

        currentOutputStream = fileOutputStream;
    }
}
