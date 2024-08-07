// Created: 18.09.2019
package de.freese.arser.core.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public final class ArserUtils {
    /**
     * io.netty.handler.codec.http.HttpHeaderNames
     */
    public static final String HTTP_HEADER_CONTENT_LENGTH = "content-length";
    /**
     * io.netty.handler.codec.http.HttpHeaderNames
     */
    public static final String HTTP_HEADER_CONTENT_TYPE = "content-type";
    /**
     * io.netty.handler.codec.http.HttpHeaderNames
     */
    public static final String HTTP_HEADER_SERVER = "server";
    /**
     * io.netty.handler.codec.http.HttpHeaderNames
     */
    public static final String HTTP_HEADER_USER_AGENT = "user-agent";
    /**
     * io.netty.handler.codec.http.HttpResponseStatus
     */
    public static final int HTTP_NOT_FOUND = HttpURLConnection.HTTP_NOT_FOUND;
    /**
     * io.netty.handler.codec.http.HttpResponseStatus
     */
    public static final int HTTP_OK = HttpURLConnection.HTTP_OK;
    /**
     * io.netty.handler.codec.http.HttpResponseStatus
     */
    public static final int HTTP_SERVICE_UNAVAILABLE = HttpURLConnection.HTTP_UNAVAILABLE;
    public static final Pattern PATTERN_SNAPSHOT_TIMESTAMP = Pattern.compile("\\d{8}\\.\\d{6}-\\d+");
    public static final String SERVER_NAME = "ARtifact-SERvice";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArserUtils.class);

    //    private static final FileNameMap FILE_NAME_MAP = URLConnection.getFileNameMap();
    //
    //    private static final MimetypesFileTypeMap MIMETYPES_FILE_TYPE_MAP = new MimetypesFileTypeMap();

    public static String getContentType(final String fileName) {
        return "application/octet-stream";
        //        return MIMETYPES_FILE_TYPE_MAP.getContentType(fileName);
        //        return FILE_NAME_MAP.getContentTypeFor(fileName);
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader classLoader = null;

        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        catch (Exception ex) {
            // Cannot access thread context ClassLoader - falling back...
        }

        if (classLoader == null) {
            // No thread context class loader -> use class loader of this class.
            try {
                classLoader = ArserUtils.class.getClassLoader();
            }
            catch (Exception ex) {
                // Cannot access class loader of this class.
            }
        }

        if (classLoader == null) {
            // getClassLoader() returning null indicates the bootstrap ClassLoader
            try {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            catch (Exception ex) {
                // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
            }
        }

        return classLoader;
    }

    public static void setupProxy() throws UnknownHostException {
        final String domain = System.getenv("userdomain");

        if (domain != null && !domain.equals(System.getProperty("DOMAIN"))) {
            return;
        }

        final InetAddress address = InetAddress.getLocalHost();
        final String canonicalHostName = address.getCanonicalHostName();

        if (canonicalHostName != null && !canonicalHostName.endsWith(System.getProperty("HOST"))) {
            return;
        }

        final String proxyHost = System.getProperty("PROXY");
        final String proxyPort = "8080";
        final String nonProxyHosts = "localhost|127.*|[::1]|*.DOMAIN";
        final String userID = System.getProperty("user.name");
        final String password = System.getProperty("PROXY_PASS");

        System.setProperty("java.net.useSystemProxies", "true");

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        System.setProperty("http.nonProxyHosts", nonProxyHosts);
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);
        System.setProperty("https.nonProxyHosts", nonProxyHosts);

        // For Exception: java.net.ProtocolException: Server redirected too many times (20)
        // System.setProperty("http.maxRedirects", "99");
        //
        // Default cookie manager.
        // CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        // String encoded = new String(Base64.encodeBase64((getHTTPUsername() + ":" + getHTTPPassword()).getBytes()));
        // con.setRequestProperty("Proxy-Authorization", "Basic " + encoded);

        java.net.Authenticator.setDefault(new java.net.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userID, password.toCharArray());
            }
        });

        // Test
        if (Boolean.getBoolean("java.net.useSystemProxies")) {
            try {
                final URI uri = URI.create("https://www.google.de");
                // final URI uri = URI.create("https://search.maven.org");

                // Available Proxies for a URI.
                final List<Proxy> proxies = ProxySelector.getDefault().select(uri);
                proxies.forEach(p -> LOGGER.info("{}", p));

                // SocketAddress proxyAddress = new InetSocketAddress("194.114.63.23", 8080);
                // final Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
                final Proxy proxy = proxies.get(0);

                final URLConnection connection = uri.toURL().openConnection(proxy);
                // final URLConnection connection = url.openConnection();

                try (InputStream response = connection.getInputStream();
                     InputStreamReader inputStreamReader = new InputStreamReader(response, StandardCharsets.UTF_8);
                     BufferedReader in = new BufferedReader(inputStreamReader)) {
                    String line = null;

                    while ((line = in.readLine()) != null) {
                        LOGGER.info(line);
                    }
                }

                ((HttpURLConnection) connection).disconnect();
            }
            catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    public static void shutdown(final ExecutorService executorService) {
        shutdown(executorService, LoggerFactory.getLogger(ArserUtils.class));
    }

    public static void shutdown(final ExecutorService executorService, final Logger logger) {
        logger.info("shutdown ExecutorService");

        if (executorService == null) {
            logger.warn("ExecutorService is null");

            return;
        }

        executorService.shutdown();

        try {
            // Wait a while for existing tasks to terminate.
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Timed out while waiting for ExecutorService");

                // Cancel currently executing tasks.
                for (Runnable remainingTask : executorService.shutdownNow()) {
                    if (remainingTask instanceof Future<?> f) {
                        f.cancel(true);
                    }
                }

                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("ExecutorService did not terminate");
                }
                else {
                    logger.info("ExecutorService terminated");
                }
            }
            else {
                logger.info("ExecutorService terminated");
            }
        }
        catch (InterruptedException iex) {
            logger.warn("Interrupted while waiting for ExecutorService");

            // (Re-)Cancel if current thread also interrupted.
            executorService.shutdownNow();

            // Preserve interrupt status.
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @return String, z.B. '___,_ MB'
     */
    public static String toHumanReadable(final long size) {
        double value = Math.abs(size);

        if (value < 1024D) {
            return size + " B";
        }

        value /= 1024D;

        if (value < 1024D) {
            return String.format("%.1f %s", value, "KB");
        }

        value /= 1024D;

        if (value < 1024D) {
            return String.format("%.1f %s", value, "MB");
        }

        value /= 1024D;

        return String.format("%.1f %s", value, "GB");

        // final CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        //
        // while (value > 1024D)
        // {
        // value /= 1024D;
        // ci.next();
        // }
        //
        // return String.format("%.1f %cB", value, ci.previous());
    }

    private ArserUtils() {
        super();
    }
}
