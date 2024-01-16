// Created: 22.07.23
package de.freese.arser.core.server.jre;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.freese.arser.core.component.AbstractComponent;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.RepositoryResponse;
import de.freese.arser.core.utils.HttpMethod;
import de.freese.arser.core.utils.ProxyUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpServerHandler extends AbstractComponent implements HttpHandler {

    private static final String SERVER_NAME = "ARtifact-SERver";

    private final Map<String, Repository> contextRoots;

    JreHttpServerHandler(final Map<String, Repository> contextRoots) {
        super();

        this.contextRoots = checkNotNull(contextRoots, "contextRoots");
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        final HttpMethod httpMethod = HttpMethod.get(exchange.getRequestMethod());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("{}: {}", httpMethod, exchange.getRequestURI());

            if (getLogger().isTraceEnabled()) {
                exchange.getRequestHeaders().forEach((key, value) -> getLogger().trace("{} = {}", key, value));
            }
        }

        final URI requestUri = exchange.getRequestURI();
        final String contextRoot = getContextRoot(requestUri);
        final URI resource = stripContextRoot(requestUri, contextRoot);
        final Repository repository = contextRoots.get(contextRoot);

        if (repository == null) {
            getLogger().error("No Repository not found for contextRoot: {}", contextRoot);

            consumeAndCloseRequestStream(exchange);

            exchange.getResponseHeaders().add(ProxyUtils.HTTP_HEADER_SERVER, SERVER_NAME);
            exchange.sendResponseHeaders(ProxyUtils.HTTP_SERVICE_UNAVAILABLE, 0);
            exchange.getResponseBody().close();
            exchange.close();

            return;
        }

        if (!repository.supports(httpMethod)) {
            getLogger().error("Repository does not support HttpMethod: {} - {}", repository.getName(), httpMethod);

            consumeAndCloseRequestStream(exchange);

            exchange.getResponseHeaders().add(ProxyUtils.HTTP_HEADER_SERVER, SERVER_NAME);
            exchange.sendResponseHeaders(ProxyUtils.HTTP_SERVICE_UNAVAILABLE, 0);
            exchange.getResponseBody().close();
            exchange.close();

            return;
        }

        try {
            if (HttpMethod.GET.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleGet(exchange, resource, repository);
            }
            else if (HttpMethod.HEAD.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleHead(exchange, resource, repository);
            }
            else if (HttpMethod.PUT.equals(httpMethod)) {
                handlePut(exchange, resource, repository);
            }
            else {
                getLogger().error("unknown method: {} from {}", httpMethod, exchange.getRemoteAddress());

                consumeAndCloseRequestStream(exchange);

                exchange.getResponseHeaders().add(ProxyUtils.HTTP_HEADER_SERVER, SERVER_NAME);
                exchange.sendResponseHeaders(ProxyUtils.HTTP_SERVICE_UNAVAILABLE, 0);
            }
        }
        catch (final IOException ex) {
            getLogger().error(ex.getMessage(), ex);
            throw ex;
        }
        catch (final Exception ex) {
            getLogger().error(ex.getMessage(), ex);
            throw new IOException(ex);
        }
        finally {
            exchange.getResponseBody().close();
            exchange.close();
        }
    }

    /**
     * See Documentation of {@link HttpExchange}.
     */
    protected void consumeAndCloseRequestStream(final HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            inputStream.transferTo(OutputStream.nullOutputStream());
        }
    }

    protected String getContextRoot(final URI uri) {
        String path = uri.getPath();
        path = path.substring(1);

        return path.substring(0, path.indexOf('/'));
    }

    protected void handleGet(final HttpExchange exchange, final URI resource, final Repository repository) throws Exception {
        final RepositoryResponse repositoryResponse = repository.getInputStream(resource);

        if (repositoryResponse == null) {
            final String message = "File not found: " + resource.toString();
            final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(ProxyUtils.HTTP_NOT_FOUND, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                exchange.getResponseBody().write(bytes);

                outputStream.flush();
            }

            return;
        }

        final long fileLength = repositoryResponse.getContentLength();

        exchange.getResponseHeaders().add(ProxyUtils.HTTP_HEADER_SERVER, SERVER_NAME);
        exchange.getResponseHeaders().add(ProxyUtils.HTTP_HEADER_CONTENT_TYPE, ProxyUtils.getContentType(repositoryResponse.getFileName()));
        exchange.sendResponseHeaders(ProxyUtils.HTTP_OK, fileLength);

        try (OutputStream outputStream = new BufferedOutputStream(exchange.getResponseBody())) {
            repositoryResponse.transferTo(outputStream);

            outputStream.flush();
        }
    }

    protected void handleHead(final HttpExchange exchange, final URI resource, final Repository repository) throws Exception {
        final boolean exist = repository.exist(resource);

        final int response = exist ? ProxyUtils.HTTP_OK : ProxyUtils.HTTP_NOT_FOUND;

        exchange.getResponseHeaders().add(ProxyUtils.HTTP_HEADER_SERVER, SERVER_NAME);
        exchange.sendResponseHeaders(response, -1);
    }

    /**
     * Deploy
     **/
    protected void handlePut(final HttpExchange exchange, final URI resource, final Repository repository) throws Exception {
        try (InputStream inputStream = new BufferedInputStream(exchange.getRequestBody())) {
            repository.write(resource, inputStream);
        }

        exchange.getResponseHeaders().add(ProxyUtils.HTTP_HEADER_SERVER, SERVER_NAME);
        exchange.sendResponseHeaders(ProxyUtils.HTTP_OK, -1);
    }

    protected URI stripContextRoot(final URI uri, final String contextRoot) {
        final String path = uri.getPath().substring(contextRoot.length() + 1);

        return URI.create(path);
    }
}
