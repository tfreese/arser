// Created: 22.07.23
package de.freese.arser.core.server.jre;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.freese.arser.core.component.AbstractComponent;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.RepositoryResponse;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.core.utils.HttpMethod;

/**
 * @author Thomas Freese
 */
public class JreHttpServerHandlerForRepository extends AbstractComponent implements HttpHandler {
    private final String contextRoot;

    private final Repository repository;

    JreHttpServerHandlerForRepository(final Repository repository) {
        super();

        this.repository = checkNotNull(repository, "Repository");
        this.contextRoot = "/" + repository.getName();
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

        try {
            final URI resource = removeContextRoot(exchange.getRequestURI());

            if (HttpMethod.GET.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleGet(exchange, resource);
            }
            else if (HttpMethod.HEAD.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleHead(exchange, resource);
            }
            else if (HttpMethod.PUT.equals(httpMethod)) {
                if (!getRepository().isWriteable()) {
                    getLogger().error("Repository is not support HttpMethod: {} - {}", getRepository().getName(), httpMethod);

                    consumeAndCloseRequestStream(exchange);

                    exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
                    exchange.sendResponseHeaders(ArserUtils.HTTP_SERVICE_UNAVAILABLE, 0);
                    exchange.getResponseBody().close();
                    exchange.close();

                    return;
                }

                handlePut(exchange, resource);
            }
            else {
                getLogger().error("unknown method: {} from {}", httpMethod, exchange.getRemoteAddress());

                consumeAndCloseRequestStream(exchange);

                exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
                exchange.sendResponseHeaders(ArserUtils.HTTP_SERVICE_UNAVAILABLE, 0);
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

    protected String getContextRoot() {
        return contextRoot;
    }

    protected Repository getRepository() {
        return repository;
    }

    protected void handleGet(final HttpExchange exchange, final URI resource) throws Exception {
        final RepositoryResponse repositoryResponse = getRepository().getInputStream(resource);

        if (repositoryResponse == null) {
            final String message = "File not found: " + resource.toString();
            final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(ArserUtils.HTTP_NOT_FOUND, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                exchange.getResponseBody().write(bytes);

                outputStream.flush();
            }

            return;
        }

        final long fileLength = repositoryResponse.getContentLength();

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_CONTENT_TYPE, ArserUtils.getContentType(repositoryResponse.getFileName()));
        exchange.sendResponseHeaders(ArserUtils.HTTP_OK, fileLength);

        try (OutputStream outputStream = new BufferedOutputStream(exchange.getResponseBody())) {
            repositoryResponse.transferTo(outputStream);

            outputStream.flush();
        }
    }

    protected void handleHead(final HttpExchange exchange, final URI resource) throws Exception {
        final boolean exist = getRepository().exist(resource);

        final int response = exist ? ArserUtils.HTTP_OK : ArserUtils.HTTP_NOT_FOUND;

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(response, -1);
    }

    /**
     * Deploy
     **/
    protected void handlePut(final HttpExchange exchange, final URI resource) throws Exception {
        try (InputStream inputStream = new BufferedInputStream(exchange.getRequestBody())) {
            getRepository().write(resource, inputStream);
        }

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(ArserUtils.HTTP_OK, -1);
    }

    protected URI removeContextRoot(final URI uri) {
        final String path = uri.getPath().substring(getContextRoot().length());

        return URI.create(path);
    }
}
