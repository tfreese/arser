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

    String getFileName();

    InputStream getInputStream();

    ResourceRequest getResourceRequest();

    long transferTo(OutputStream outputStream) throws IOException;
}
