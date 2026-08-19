package de.freese.arser.serialisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import de.freese.arser.api.ArserConfig;
import de.freese.arser.repository.AbstractRepositoryConfig;
import de.freese.arser.repository.XmlTypeIgnoreMixIn;
import de.freese.arser.repository.virtual.VirtualRepositoryConfig;
import de.freese.arser.utils.JacksonMapperConfig;

/**
 * @author Thomas Freese
 * @since 19.08.26
 */
class TestFileConfig {
    @Test
    void testJsonFile() throws Exception {
        final JsonMapper jsonMapper = JacksonMapperConfig.createJsonMapper();

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config/arser-config.json")) {
            // final ArserConfig arserConfig = jsonMapper.reader().forType(ArserConfig.class).readValue(inputStream);
            // final ArserConfig arserConfig = jsonMapper.readerFor(ArserConfig.class).readValue(inputStream);
            final ArserConfig arserConfig = jsonMapper.readValue(inputStream, ArserConfig.class);

            assertNotNull(arserConfig);

            final VirtualRepositoryConfig virtualRepositoryConfig = arserConfig.virtualRepositoryConfigs().getFirst();
            assertEquals("snapshots", virtualRepositoryConfig.repositoryRefs().getFirst());
            assertEquals("releases", virtualRepositoryConfig.repositoryRefs().get(1));
            assertEquals("maven-central", virtualRepositoryConfig.repositoryRefs().getLast());
        }
    }

    @Test
    void testXmlFile() throws Exception {
        // Validation
        final URL xmlFile = Thread.currentThread().getContextClassLoader().getResource("config/arser-config.xml");
        final URL xsdFile = Thread.currentThread().getContextClassLoader().getResource("config/arser-config.xsd");

        assertNotNull(xmlFile);
        assertNotNull(xsdFile);

        // 1. Standard-Java-SchemaFactory initialisieren (W3C XML Schema).
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        final Schema schema = schemaFactory.newSchema(xsdFile);

        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

        try (InputStream inputStreamXml = xmlFile.openStream()) {
            // 2. Den Standard-StAX-Parser für das XML-Dokument vorbereiten.
            // final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(inputStreamXml);

            // // 3. Vorab-Validierung (Wirft eine SAXException, wenn das XML ungültig ist).
            final Validator validator = schema.newValidator();
            
            // validator.validate(new StAXSource(streamReader));
            validator.validate(new StreamSource(inputStreamXml));
        }

        // // Den Reader nach der Validierung zurücksetzen oder neu öffnen für Jackson.
        try (InputStream inputStreamXml = xmlFile.openStream()) {
            final XMLStreamReader validatedReader = inputFactory.createXMLStreamReader(inputStreamXml);

            final XmlMapper xmlMapper = JacksonMapperConfig.createXmlMapper(builder -> builder.addMixIn(AbstractRepositoryConfig.class, XmlTypeIgnoreMixIn.class));
            final ArserConfig arserConfig = xmlMapper.readValue(validatedReader, ArserConfig.class);

            assertNotNull(arserConfig);

            final VirtualRepositoryConfig virtualRepositoryConfig = arserConfig.virtualRepositoryConfigs().getFirst();
            assertEquals("snapshots", virtualRepositoryConfig.repositoryRefs().getFirst());
            assertEquals("releases", virtualRepositoryConfig.repositoryRefs().get(1));
            assertEquals("maven-central", virtualRepositoryConfig.repositoryRefs().getLast());
        }
    }
}
