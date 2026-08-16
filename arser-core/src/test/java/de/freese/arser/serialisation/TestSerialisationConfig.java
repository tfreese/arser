package de.freese.arser.serialisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import de.freese.arser.repository.AbstractRepositoryConfig;
import de.freese.arser.repository.XmlTypeIgnoreMixIn;
import de.freese.arser.repository.file.FileRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.virtual.VirtualRepositoryConfig;
import de.freese.arser.utils.JacksonMapperConfig;

/**
 * @author Thomas Freese
 * @since 16.08.26
 */
class TestSerialisationConfig {
    private static ObjectReader objectReaderJson;
    private static ObjectReader objectReaderXml;
    private static ObjectWriter objectWriterJson;
    private static ObjectWriter objectWriterXml;

    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static Path pathTest;

    @AfterAll
    static void afterAll() throws Exception {
        // Empty
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        final JsonMapper jsonMapper = JacksonMapperConfig.createJsonMapper();
        objectWriterJson = jsonMapper.writer();
        objectReaderJson = jsonMapper.reader();

        final XmlMapper xmlMapper = JacksonMapperConfig.createXmlMapper(builder -> builder.addMixIn(AbstractRepositoryConfig.class, XmlTypeIgnoreMixIn.class));
        objectWriterXml = xmlMapper.writer();
        objectReaderXml = xmlMapper.reader();
    }

    @Test
    void testFileRepositoryConfigJson() {
        testFileRepositoryConfig(objectWriterJson, objectReaderJson);
    }

    @Test
    void testFileRepositoryConfigXml() {
        testFileRepositoryConfig(objectWriterXml, objectReaderXml);
    }

    @Test
    void testHttpRepositoryConfigJson() {
        testHttpRepositoryConfig(objectWriterJson, objectReaderJson);
    }

    @Test
    void testHttpRepositoryConfigXml() {
        testHttpRepositoryConfig(objectWriterXml, objectReaderXml);
    }

    @Test
    void testVirtualRepositoryConfigJson() {
        testVirtualRepositoryConfig(objectWriterJson, objectReaderJson);
    }

    @Test
    void testVirtualRepositoryConfigXml() {
        testVirtualRepositoryConfig(objectWriterXml, objectReaderXml);
    }

    private void testFileRepositoryConfig(final ObjectWriter objectWriter, final ObjectReader objectReader) {
        final FileRepositoryConfig repositoryConfig = FileRepositoryConfig.builder()
                .name("maven-local")
                .uri(pathTest.toUri())
                .readOnly(false)
                .withLogging()
                .build();

        final String value = objectWriter.writeValueAsString(repositoryConfig);

        System.out.println(value);

        // LifeCycleRegistry lifeCycleRegistry = new DefaultLifeCycleRegistry();
        //
        // InjectableValues inject = new InjectableValues.Std()
        //         .addValue("lifeCycleRegistry", lifeCycleRegistry);
        //
        // final AbstractRepositoryConfig config = objectMapper.reader(inject).forType(FileRepositoryConfig.class).readValue(value);
        final AbstractRepositoryConfig config = objectReader.forType(FileRepositoryConfig.class).readValue(value);
        assertNotNull(config);
        assertEquals(repositoryConfig, config);
    }

    private void testHttpRepositoryConfig(final ObjectWriter objectWriter, final ObjectReader objectReader) {
        final HttpRepositoryConfig repositoryConfig = HttpRepositoryConfig.builder()
                .name("maven-central")
                .uri(pathTest.toUri())
                .withLogging()
                .cachingPath(pathTest)
                .connectTimeout(Duration.ofSeconds(13L))
                .maxRetries(4)
                .retryInterval(Duration.ofSeconds(2L))
                .build();

        final String value = objectWriter.writeValueAsString(repositoryConfig);

        System.out.println(value);

        final AbstractRepositoryConfig config = objectReader.forType(HttpRepositoryConfig.class).readValue(value);
        assertNotNull(config);
        assertEquals(repositoryConfig, config);
    }

    private void testVirtualRepositoryConfig(final ObjectWriter objectWriter, final ObjectReader objectReader) {
        final VirtualRepositoryConfig repositoryConfig = VirtualRepositoryConfig.builder()
                .name("virtual")
                .uri(pathTest.toUri())
                .withLogging()
                .addRepositoryRef("ref-b")
                .addRepositoryRef("ref-a")
                .build();

        final String value = objectWriter.writeValueAsString(repositoryConfig);

        System.out.println(value);

        final AbstractRepositoryConfig config = objectReader.forType(VirtualRepositoryConfig.class).readValue(value);
        assertNotNull(config);
        assertEquals(repositoryConfig, config);
    }
}
