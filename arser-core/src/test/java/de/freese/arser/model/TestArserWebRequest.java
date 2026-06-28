package de.freese.arser.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * @author Thomas Freese
 */
class TestArserWebRequest {
    private static final String RESOURCE_PATH = "public/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.pom";

    @Test
    void testArserWebRequest() {
        final ArserWebRequest arserWebRequest = ArserWebRequest.of(RESOURCE_PATH);

        assertEquals(URI.create(RESOURCE_PATH.substring(RESOURCE_PATH.indexOf("/") + 1)), arserWebRequest.getResource());
        assertEquals("public", arserWebRequest.getContextRoot());
        assertEquals("org.slf4j", arserWebRequest.getGroupId());
        assertEquals("slf4j-api", arserWebRequest.getArtifactId());
        assertEquals("2.0.18", arserWebRequest.getVersion());
    }

    @Test
    void testArserWebRequestLeadingSlash() {
        final ArserWebRequest arserWebRequest = ArserWebRequest.of("/" + RESOURCE_PATH);

        assertEquals(URI.create(RESOURCE_PATH.substring(RESOURCE_PATH.indexOf("/") + 1)), arserWebRequest.getResource());
        assertEquals("public", arserWebRequest.getContextRoot());
        assertEquals("org.slf4j", arserWebRequest.getGroupId());
        assertEquals("slf4j-api", arserWebRequest.getArtifactId());
        assertEquals("2.0.18", arserWebRequest.getVersion());
    }
}
