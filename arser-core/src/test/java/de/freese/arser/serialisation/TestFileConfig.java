package de.freese.arser.serialisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import de.freese.arser.api.ArserConfig;
import de.freese.arser.repository.virtual.VirtualRepositoryConfig;

/**
 * @author Thomas Freese
 * @since 19.08.26
 */
class TestFileConfig {
    @Test
    void testJson() throws Exception {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("arser-config.json")) {
            final ArserConfig arserConfig = ArserConfig.fromJson(inputStream);

            assertNotNull(arserConfig);

            final VirtualRepositoryConfig virtualRepositoryConfig = arserConfig.virtualRepositoryConfigs().getFirst();
            assertEquals("snapshots", virtualRepositoryConfig.repositoryRefs().getFirst());
            assertEquals("releases", virtualRepositoryConfig.repositoryRefs().get(1));
            assertEquals("maven-central", virtualRepositoryConfig.repositoryRefs().getLast());
        }
    }

    @Test
    void testXml() throws Exception {
        // Validation
        final URL xmlFile = Thread.currentThread().getContextClassLoader().getResource("arser-config.xml");
        final URL xsdFile = Thread.currentThread().getContextClassLoader().getResource("config/arser-config.xsd");

        assertNotNull(xmlFile);
        assertNotNull(xsdFile);

        final ArserConfig arserConfig = ArserConfig.fromXml(xmlFile, xsdFile);

        assertNotNull(arserConfig);

        final VirtualRepositoryConfig virtualRepositoryConfig = arserConfig.virtualRepositoryConfigs().getFirst();
        assertEquals("snapshots", virtualRepositoryConfig.repositoryRefs().getFirst());
        assertEquals("releases", virtualRepositoryConfig.repositoryRefs().get(1));
        assertEquals("maven-central", virtualRepositoryConfig.repositoryRefs().getLast());
    }
}
