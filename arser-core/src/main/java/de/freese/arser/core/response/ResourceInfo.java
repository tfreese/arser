// Created: 27 Okt. 2024
package de.freese.arser.core.response;

import de.freese.arser.core.request.ResourceRequest;

/**
 * @author Thomas Freese
 */
public record ResourceInfo(ResourceRequest resourceRequest, long contentLength) {
    public String fileName() {
        final String path = resourceRequest().getResource().getPath();
        final int lastSlashIndex = path.lastIndexOf('/');

        return path.substring(lastSlashIndex + 1);
    }
}
