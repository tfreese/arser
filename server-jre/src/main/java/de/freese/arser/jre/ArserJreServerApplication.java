// Created: 27.07.23
package de.freese.arser.jre;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.freese.arser.instance.ArserInstance;
import de.freese.arser.instance.ArserInstanceFactory;
import de.freese.arser.jre.server.JreHttpServer;

/**
 * @author Thomas Freese
 */
public final class ArserJreServerApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArserJreServerApplication.class);

    public static void main(final String[] args) {
        // if (LOGGER.isInfoEnabled()) {
        //     LOGGER.info("Working Directory: {}", System.getProperty("user.dir"));
        //     LOGGER.info("Process User: {}", System.getProperty("user.name"));
        // }

        // Redirect Java-Util-Logger to Slf4J.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        if (LoggerFactory.getLogger("jdk.httpclient.HttpClient").isDebugEnabled()) {
            // System.setProperty("jdk.httpclient.HttpClient.log", "all");
            System.setProperty("jdk.httpclient.HttpClient.log", "requests");
        }

        try (InputStream inputStreamSchema = ClassLoader.getSystemResourceAsStream("xsd/arser-config.xsd");
             InputStream inputStreamXml = ClassLoader.getSystemResourceAsStream("arser-config.xml")) {
            final ArserInstance arserInstance = ArserInstanceFactory.createArserInstanceFromXml(inputStreamSchema, inputStreamXml);

            // Server at last
            final JreHttpServer proxyServer = new JreHttpServer(arserInstance);
            proxyServer.start();

            // ArserUtils.setupProxy();

            // Logger root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            // root.setLevel(Level.INFO);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    arserInstance.shutdown();
                    proxyServer.stop();
                }
                catch (final Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }, "Shutdown"));
        }
        catch (final Exception ex) {
            LOGGER.error(ex.getMessage(), ex);

            System.exit(-1);
        }
    }

    private ArserJreServerApplication() {
        super();
    }
}
