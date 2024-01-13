// Created: 22.07.23
package de.freese.arser.core;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.freese.arser.config.ApplicationConfig;
import de.freese.arser.config.Repositories;
import de.freese.arser.core.component.JreHttpClientComponent;
import de.freese.arser.core.lifecycle.LifecycleManager;
import de.freese.arser.core.repository.RepositoryManager;
import de.freese.arser.core.server.ProxyServer;
import de.freese.arser.core.server.jre.JreHttpServer;
import de.freese.arser.core.utils.ProxyUtils;

/**
 * @author Thomas Freese
 */
//@SuppressWarnings("static")
public final class ArserLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArserLauncher.class);

    public static void main(final String[] args) throws Exception {
        LOGGER.info("Working Directory: {}", System.getProperty("user.dir"));
        LOGGER.info("Process User: {}", System.getProperty("user.name"));

        // Redirect Java-Util-Logger to Slf4J.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        if (LoggerFactory.getLogger("jdk.httpclient.HttpClient").isDebugEnabled()) {
            //            System.setProperty("jdk.httpclient.HttpClient.log", "all");
            System.setProperty("jdk.httpclient.HttpClient.log", "requests");
        }

        final URI configUri = findConfigFile(args);

        //        if (!Files.exists(Paths.get(configUri))) {
        //            LOGGER.error("arser config file not exist: {}", configUri);
        //            return;
        //        }

        ProxyUtils.getDefaultClassLoader();
        final URL url = ClassLoader.getSystemResource("xsd/arser-config.xsd");
        LOGGER.info("XSD-Url: {}", url);
        final Source schemaFile = new StreamSource(url.openStream());

        final Source xmlFile = new StreamSource(configUri.toURL().openStream());

        // Validate Schema.
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        final Schema schema = schemaFactory.newSchema(schemaFile);
        //        final Validator validator = schema.newValidator();
        //        validator.validate(xmlFile);

        final JAXBContext jaxbContext = JAXBContext.newInstance(ApplicationConfig.class.getPackageName());
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(schema);
        final ApplicationConfig applicationConfig = (ApplicationConfig) unmarshaller.unmarshal(xmlFile);

        // ProxyUtils.setupProxy();

        //        Logger root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        //        root.setLevel(Level.INFO);

        final LifecycleManager lifecycleManager = new LifecycleManager();

        final JreHttpClientComponent httpClientComponent = new JreHttpClientComponent(applicationConfig.getClientConfig());
        lifecycleManager.add(httpClientComponent);

        final RepositoryManager repositoryManager = new RepositoryManager();
        final Repositories repositories = applicationConfig.getRepositories();

        // LocalRepository
        repositories.getLocals().forEach(localRepoConfig -> RepositoryBuilder.buildLocal(localRepoConfig, lifecycleManager, repositoryManager));

        // RemoteRepository
        repositories.getRemotes().forEach(remoteRepoConfig -> RepositoryBuilder.buildRemote(remoteRepoConfig, lifecycleManager, repositoryManager, httpClientComponent));

        // VirtualRepository
        repositories.getVirtuals().forEach(virtualRepoConfig -> RepositoryBuilder.buildVirtual(virtualRepoConfig, lifecycleManager, repositoryManager));

        // Server at last
        final ProxyServer proxyServer = new JreHttpServer().setConfig(applicationConfig.getServerConfig());
        repositoryManager.getRepositories().forEach(repo -> proxyServer.addContextRoot(repo.getName(), repo));
        lifecycleManager.add(proxyServer);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                lifecycleManager.stop();
            }
            catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }, "Shutdown"));

        try {
            lifecycleManager.start();
        }
        catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);

            System.exit(-1);
        }
    }

    private static URI findConfigFile(final String[] args) throws Exception {
        LOGGER.info("Try to find arser-config.xml");

        if (args != null && args.length == 2) {
            final String parameter = args[0];

            if ("-arser.config".equals(parameter)) {
                return Paths.get(args[1]).toUri();
            }
        }

        final String propertyValue = System.getProperty("arser.config");

        if (propertyValue != null) {
            return Paths.get(propertyValue).toUri();
        }

        final String envValue = System.getenv("arser.config");

        if (envValue != null) {
            return Paths.get(envValue).toUri();
        }

        ProxyUtils.getDefaultClassLoader();
        final URL url = ClassLoader.getSystemResource("arser-config.xml");

        if (url != null) {
            return url.toURI();
        }

        final Path path = Path.of("arser-config.xml");

        if (Files.exists(path)) {
            return path.toUri();
        }

        LOGGER.error("no arser config file found");
        LOGGER.error("define it as programm argument: -arser.config <ABSOLUTE_PATH>/arser-config.xml");
        LOGGER.error("or as system property: -Darser.config=<ABSOLUTE_PATH>/arser-config.xml");
        LOGGER.error("or as environment variable: set/export arser.config=<ABSOLUTE_PATH>/arser-config.xml");
        LOGGER.error("or in Classpath");
        LOGGER.error("or in directory of the Proxy-Jar.");

        throw new IllegalStateException("no arser config file found");
    }

    private ArserLauncher() {
        super();
    }
}
