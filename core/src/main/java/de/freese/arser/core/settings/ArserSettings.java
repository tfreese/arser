// Created: 18.01.24
package de.freese.arser.core.settings;

import java.io.InputStream;

/**
 * @author Thomas Freese
 */
public interface ArserSettings {
    static ArserSettings fromXml(final InputStream inputStream) throws Exception {
        //        final URL url = ClassLoader.getSystemResource("xsd/arser-config.xsd");
        //        final Source schemaFile = new StreamSource(url.openStream());
        //
        //        final Source xmlFile = new StreamSource(inputStream);
        //
        //        // Validate Schema.
        //        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        //        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        //        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        //
        //        final Schema schema = schemaFactory.newSchema(schemaFile);
        //        //        final Validator validator = schema.newValidator();
        //        //        validator.validate(xmlFile);
        //
        //        final JAXBContext jaxbContext = JAXBContext.newInstance(ApplicationConfig.class.getPackageName());
        //        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        //        unmarshaller.setSchema(schema);
        //        final ApplicationConfig applicationConfig = (ApplicationConfig) unmarshaller.unmarshal(xmlFile);
        //
        return null;
    }
}
