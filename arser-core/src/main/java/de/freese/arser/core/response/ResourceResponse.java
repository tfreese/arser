// Created: 17.01.24
package de.freese.arser.core.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public interface ResourceResponse {

    long getContentLength();

    default String getFileName() {
        final String path = getResourceRequest().getResource().getPath();
        final int lastSlashIndex = path.lastIndexOf('/');

        return path.substring(lastSlashIndex + 1);
    }

    InputStream getInputStream();

    ResourceRequest getResourceRequest();

    void transferTo(OutputStream outputStream) throws IOException;
}
