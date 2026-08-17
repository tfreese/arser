package de.freese.arser.serialisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
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

import de.freese.arser.api.ArserConfig;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.config.ThreadPoolConfig;
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
    static void afterAll() {
        // Empty
    }

    @BeforeAll
    static void beforeAll() {
        final JsonMapper jsonMapper = JacksonMapperConfig.createJsonMapper();
        objectWriterJson = jsonMapper.writer();
        objectReaderJson = jsonMapper.reader();

        final XmlMapper xmlMapper = JacksonMapperConfig.createXmlMapper(builder -> builder.addMixIn(AbstractRepositoryConfig.class, XmlTypeIgnoreMixIn.class));
        objectWriterXml = xmlMapper.writer();
        objectReaderXml = xmlMapper.reader();

        // Validation
        // // 1. Standard-Java-SchemaFactory initialisieren (W3C XML Schema).
        // SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        // schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        //
        // Schema schema = schemaFactory.newSchema(xsdFile);
        //
        // // 2. Den Standard-StAX-Parser für das XML-Dokument vorbereiten.
        // XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new java.io.FileInputStream(xmlFile));
        //
        // // 3. Vorab-Validierung (Wirft eine SAXException, wenn das XML ungültig ist).
        // Validator validator = schema.newValidator();
        // validator.validate(new StAXSource(streamReader));
        //
        // // Den Reader nach der Validierung zurücksetzen oder neu öffnen für Jackson.
        // XMLStreamReader validatedReader = inputFactory.createXMLStreamReader(new java.io.FileInputStream(xmlFile));
        // xmlMapper.readValue(validatedReader, ArserConfig.class);
    }

    @Test
    void testArserConfigJson() {
        testArserConfig(objectWriterJson, objectReaderJson);
    }

    @Test
    void testArserConfigXml() {
        testArserConfig(objectWriterXml, objectReaderXml);
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
    void testServerConfigJson() {
        testServerConfig(objectWriterJson, objectReaderJson);
    }

    @Test
    void testServerConfigXml() {
        testServerConfig(objectWriterXml, objectReaderXml);
    }

    @Test
    void testThreadPoolConfigJson() {
        testThreadPoolConfig(objectWriterJson, objectReaderJson);
    }

    @Test
    void testThreadPoolConfigXml() {
        testThreadPoolConfig(objectWriterXml, objectReaderXml);
    }

    @Test
    void testVirtualRepositoryConfigJson() {
        testVirtualRepositoryConfig(objectWriterJson, objectReaderJson);
    }

    @Test
    void testVirtualRepositoryConfigXml() {
        testVirtualRepositoryConfig(objectWriterXml, objectReaderXml);
    }

    private void testArserConfig(final ObjectWriter objectWriter, final ObjectReader objectReader) {
        final ArserConfig arserConfig = ArserConfig.builder()
                .serverConfig(ServerConfig.builder()
                        .port(8484)
                        .threadPoolConfig(ThreadPoolConfig.builder()
                                .namePattern("http-%d")
                                .coreSize(1)
                                .maxSize(5)
                                .build())
                        .build())
                .addFileRepositoryConfig(FileRepositoryConfig.builder()
                        .name("maven-local")
                        .uri(pathTest.toUri())
                        .readOnly(false)
                        .withLogging()
                        .build())
                .addHttpRepositoryConfig(HttpRepositoryConfig.builder()
                        .name("maven-central")
                        .uri(URI.create("https://repo1.maven.org/maven2"))
                        .withLogging()
                        .cachingPath(pathTest)
                        .connectTimeout(Duration.ofSeconds(13L))
                        .maxRetries(4)
                        .retryInterval(Duration.ofSeconds(2L))
                        .build())
                .addVirtualRepositoryConfig(VirtualRepositoryConfig.builder()
                        .name("virtual")
                        .uri(URI.create("virtual"))
                        .withLogging()
                        .addRepositoryRef("maven-local")
                        .addRepositoryRef("maven-central")
                        .build())
                .build();

        final String value = objectWriter.writeValueAsString(arserConfig);

        System.out.println(value);

        final ArserConfig config = objectReader.forType(ArserConfig.class).readValue(value);
        assertNotNull(config);
        assertEquals(arserConfig, config);
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
                .uri(URI.create("https://repo1.maven.org/maven2"))
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

    private void testServerConfig(final ObjectWriter objectWriter, final ObjectReader objectReader) {
        final ServerConfig serverConfig = ServerConfig.builder()
                .port(8484)
                .threadPoolConfig(ThreadPoolConfig.builder()
                        .namePattern("http-%d")
                        .coreSize(1)
                        .maxSize(5)
                        .build())
                .build();

        final String value = objectWriter.writeValueAsString(serverConfig);

        System.out.println(value);

        final ServerConfig config = objectReader.forType(ServerConfig.class).readValue(value);
        assertNotNull(config);
        assertEquals(serverConfig, config);
    }

    private void testThreadPoolConfig(final ObjectWriter objectWriter, final ObjectReader objectReader) {
        final ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.builder()
                .namePattern("http-%d")
                .coreSize(1)
                .maxSize(5)
                .build();

        final String value = objectWriter.writeValueAsString(threadPoolConfig);

        System.out.println(value);

        final ThreadPoolConfig config = objectReader.forType(ThreadPoolConfig.class).readValue(value);
        assertNotNull(config);
        assertEquals(threadPoolConfig, config);
    }

    private void testVirtualRepositoryConfig(final ObjectWriter objectWriter, final ObjectReader objectReader) {
        final VirtualRepositoryConfig repositoryConfig = VirtualRepositoryConfig.builder()
                .name("virtual")
                .uri(URI.create("virtual"))
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
