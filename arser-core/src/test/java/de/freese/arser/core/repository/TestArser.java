// Created: 21 Dez. 2024
package de.freese.arser.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.Test;

import de.freese.arser.Arser;

/**
 * @author Thomas Freese
 */
class TestArser {
    @Test
    void testAddRepositoryThatExist() {
        final Arser arser = new Arser();
        arser.addRepository(new JreHttpClientRemoteRepository("a", URI.create("https://a")));
        arser.addRepository(new JreHttpClientRemoteRepository("b", URI.create("https://b")));

        final Repository repository = new JreHttpClientRemoteRepository("a", URI.create("https://a"));

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> arser.addRepository(repository));

        assertNotNull(exception);
        assertEquals("repository already exist for contextRoot: a", exception.getMessage());
    }

    @Test
    void testGetRepository() {
        final Arser arser = new Arser();
        arser.addRepository(new JreHttpClientRemoteRepository("a", URI.create("https://a")));
        arser.addRepository(new JreHttpClientRemoteRepository("b", URI.create("https://b")));

        Repository repository = arser.getRepository("a");
        assertNotNull(repository);
        assertEquals("a", repository.getContextRoot());

        repository = arser.getRepository("b");
        assertNotNull(repository);
        assertEquals("b", repository.getContextRoot());
    }
}
