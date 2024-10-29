// Created: 18.01.24
package de.freese.arser.core.settings;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import de.freese.arser.config.HttpClientConfig;
import de.freese.arser.config.LocalRepoConfig;
import de.freese.arser.config.RemoteRepoConfig;
import de.freese.arser.config.ServerConfig;
import de.freese.arser.config.VirtualRepoConfig;

/**
 * @author Thomas Freese
 */
public final class ArserSettings {
    public static ArserSettings fromEmpty() {
        return new ArserSettings();
    }

    public static ArserSettings fromXml(final InputStream inputStream) throws Exception {
        final URL url = ClassLoader.getSystemResource("xsd/arser-config.xsd");
        final Source schemaFile = new StreamSource(url.openStream());

        final Source xmlFile = new StreamSource(inputStream);

        // Validate Schema.
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        final Schema schema = schemaFactory.newSchema(schemaFile);

        // Validation either by Validator or Unmarshaller, otherwise the Stream is closed when parsing.
        // final Validator validator = schema.newValidator();
        // validator.validate(xmlFile);

        final JAXBContext jaxbContext = JAXBContext.newInstance(de.freese.arser.config.ArserSettings.class.getPackageName());
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(schema);

        final de.freese.arser.config.ArserSettings settings = (de.freese.arser.config.ArserSettings) unmarshaller.unmarshal(xmlFile);

        return new ArserSettings(settings);
    }

    private final HttpClientConfig httpClientConfig;
    private final List<LocalRepoConfig> localRepoConfigs = new ArrayList<>();
    private final List<RemoteRepoConfig> remoteRepoConfigs = new ArrayList<>();
    private final ServerConfig serverConfig;
    private final List<VirtualRepoConfig> virtualRepoConfigs = new ArrayList<>();
    private final Path workingDir;

    private ArserSettings() {
        super();

        workingDir = Path.of(System.getProperty("java.io.tmpdir"), "arser");
        httpClientConfig = new HttpClientConfig();
        serverConfig = new ServerConfig();
    }

    private ArserSettings(final de.freese.arser.config.ArserSettings settings) {
        super();

        Objects.requireNonNull(settings, "settings required");

        workingDir = Path.of(URI.create(settings.getWorkingDir()));
        httpClientConfig = settings.getHttpClientConfig();
        serverConfig = settings.getServerConfig();

        settings.getRepositories().getRemotes().forEach(this::addRemoteRepository);
        settings.getRepositories().getLocals().forEach(this::addLocalRepository);
        settings.getRepositories().getVirtuals().forEach(this::addVirtualRepository);
    }

    public void addLocalRepository(final LocalRepoConfig config) {
        localRepoConfigs.add(config);
    }

    public void addRemoteRepository(final RemoteRepoConfig config) {
        remoteRepoConfigs.add(config);
    }

    public void addVirtualRepository(final VirtualRepoConfig config) {
        virtualRepoConfigs.add(config);
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public List<LocalRepoConfig> getLocalRepositories() {
        return List.copyOf(localRepoConfigs);
    }

    public List<RemoteRepoConfig> getRemoteRepositories() {
        return List.copyOf(remoteRepoConfigs);
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public List<VirtualRepoConfig> getVirtualRepositories() {
        return List.copyOf(virtualRepoConfigs);
    }

    public Path getWorkingDir() {
        return workingDir;
    }
}
