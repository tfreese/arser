// Created: 17.01.24
package de.freese.arser.core.response;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public class DefaultResourceResponse extends AbstractResourceResponse {

    public DefaultResourceResponse(final ResourceRequest request, final long contentLength, final InputStream inputStream) {
        super(request, contentLength, inputStream);
    }

    @Override
    public void transferTo(final OutputStream outputStream) throws IOException {
        if (getInputStream() instanceof BufferedInputStream) {
            try (InputStream is = getInputStream()) {
                is.transferTo(outputStream);
            }
        }
        else {
            try (InputStream is = new BufferedInputStream(getInputStream())) {
                is.transferTo(outputStream);
            }
        }
    }
}
