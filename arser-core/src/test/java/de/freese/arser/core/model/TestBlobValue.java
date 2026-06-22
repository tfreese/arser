package de.freese.arser.core.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * @author Thomas Freese
 */
class TestBlobValue {
    private static final byte[] BLOB = new byte[]{1, 2, 3, 4, 5};

    @Test
    void testBlobValueMemory() throws Exception {
        try (BlobValue blobValue = BlobValue.of(BLOB)) {
            assertEquals(BLOB.length, blobValue.getContentLength());

            try (InputStream inputStream = blobValue.getInputStream()) {
                assertNotNull(inputStream);
                assertInstanceOf(ByteArrayInputStream.class, inputStream);
                assertArrayEquals(BLOB, inputStream.readAllBytes());
            }
        }
    }

    @Test
    void testBlobValueTempFile() throws Exception {
        try (BlobValue blobValue = BlobValue.of(3, BLOB)) {
            assertEquals(BLOB.length, blobValue.getContentLength());

            try (InputStream inputStream = blobValue.getInputStream()) {
                assertNotNull(inputStream);
                assertInstanceOf(BufferedInputStream.class, inputStream);
                assertArrayEquals(BLOB, inputStream.readAllBytes());
            }
        }
    }
}
