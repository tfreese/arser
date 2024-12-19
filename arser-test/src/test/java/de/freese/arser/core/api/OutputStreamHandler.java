// Created: 19 Dez. 2024
package de.freese.arser.core.api;

import java.io.OutputStream;

/**
 * @author Thomas Freese
 */
public interface OutputStreamHandler {
    void handle(OutputStream outputStream, int contentLength);
}
