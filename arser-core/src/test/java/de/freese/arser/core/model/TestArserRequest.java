package de.freese.arser.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * @author Thomas Freese
 */
class TestArserRequest {
    private static final String RESOURCE_PATH = "org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom";

    @Test
    void testArserRequest() {
        final ArserRequest arserRequest = ArserRequest.of(RESOURCE_PATH);

        assertEquals(URI.create(RESOURCE_PATH), arserRequest.getResource());
        assertEquals("org.slf4j", arserRequest.getGroupId());
        assertEquals("slf4j-api", arserRequest.getArtifactId());
        assertEquals("2.0.18", arserRequest.getVersion());
    }

    @Test
    void testArserRequestLeadingSlash() {
        final ArserRequest arserRequest = ArserRequest.of("/" + RESOURCE_PATH);

        assertEquals(URI.create(RESOURCE_PATH), arserRequest.getResource());
        assertEquals("org.slf4j", arserRequest.getGroupId());
        assertEquals("slf4j-api", arserRequest.getArtifactId());
        assertEquals("2.0.18", arserRequest.getVersion());
    }
}
