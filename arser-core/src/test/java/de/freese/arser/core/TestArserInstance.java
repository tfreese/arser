// Created: 21 Dez. 2024
package de.freese.arser.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import de.freese.arser.config.ArserConfig;
import de.freese.arser.config.RemoteRepositoryConfig;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.instance.ArserInstance;
import de.freese.arser.instance.ArserInstanceFactory;

/**
 * @author Thomas Freese
 */
class TestArserInstance {
    @AfterAll
    static void afterAll() {
        ArserInstanceFactory.shutdownAll();
    }

    @Test
    void testAddRepositoryThatExist() {
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> ArserInstanceFactory.createArserInstance(ArserConfig.builder()
                .addRemoteRepository(RemoteRepositoryConfig.builder()
                        .contextRoot("a")
                        .uri(URI.create("https://a"))
                        .build())
                .addRemoteRepository(RemoteRepositoryConfig.builder()
                        .contextRoot("a")
                        .uri(URI.create("https://a"))
                        .build())
                .build()));

        assertNotNull(exception);
        assertEquals("repository already exist for contextRoot: a", exception.getMessage());
    }

    @Test
    void testGetRepository() {
        final ArserInstance arserInstance = ArserInstanceFactory.createArserInstance(ArserConfig.builder()
                .addRemoteRepository(RemoteRepositoryConfig.builder()
                        .contextRoot("a")
                        .uri(URI.create("https://a"))
                        .build())
                .addRemoteRepository(RemoteRepositoryConfig.builder()
                        .contextRoot("b")
                        .uri(URI.create("https://b"))
                        .build())
                .build());

        Repository repository = arserInstance.getRepository("a");
        assertNotNull(repository);
        assertEquals("a", repository.getContextRoot());

        repository = arserInstance.getRepository("b");
        assertNotNull(repository);
        assertEquals("b", repository.getContextRoot());

        arserInstance.shutdown();
    }
}
