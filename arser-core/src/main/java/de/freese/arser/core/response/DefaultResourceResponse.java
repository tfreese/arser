// Created: 06 Mai 2025
package de.freese.arser.core.response;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import de.freese.arser.core.utils.ThrowingSupplier;

/**
 * @author Thomas Freese
 */
public class DefaultResourceResponse extends AbstractResourceResponse {
    private final ThrowingSupplier<InputStream, Exception> inputStreamSupplier;

    public DefaultResourceResponse(final long contentLength, final ThrowingSupplier<InputStream, Exception> inputStreamSupplier) {
        super(contentLength);

        this.inputStreamSupplier = Objects.requireNonNull(inputStreamSupplier, "inputStreamSupplier required");
    }

    @Override
    public void transferTo(final OutputStream outputStream) throws Exception {
        try (InputStream inputStream = inputStreamSupplier.get()) {
            if (inputStream instanceof BufferedInputStream) {
                inputStream.transferTo(outputStream);
            }
            else {
                try (InputStream is = new BufferedInputStream(inputStream)) {
                    is.transferTo(outputStream);
                }
            }
        }
    }
}
