// Created: 22.07.23
package de.freese.arser.core.server.jre;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.freese.arser.core.component.AbstractComponent;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.request.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.core.utils.HttpMethod;

/**
 * @author Thomas Freese
 */
public class JreHttpServerHandler extends AbstractComponent implements HttpHandler {
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

        final ResourceRequest resourceRequest = ResourceRequest.of(exchange.getRequestURI());
        final Repository repository = contextRoots.get(resourceRequest.getContextRoot());

        if (repository == null) {
            getLogger().error("No Repository not found for contextRoot: {}", resourceRequest.getContextRoot());

            consumeAndCloseRequestStream(exchange);

            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
            exchange.sendResponseHeaders(ArserUtils.HTTP_SERVICE_UNAVAILABLE, 0);
            exchange.getResponseBody().close();
            exchange.close();

            return;
        }

        try {
            if (HttpMethod.GET.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleGet(exchange, resourceRequest, repository);
            }
            else if (HttpMethod.HEAD.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleHead(exchange, resourceRequest, repository);
            }
            else if (HttpMethod.PUT.equals(httpMethod)) {
                if (!repository.isWriteable()) {
                    getLogger().error("Repository is not writeable: {} - {}", repository.getName());

                    consumeAndCloseRequestStream(exchange);

                    exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
                    exchange.sendResponseHeaders(ArserUtils.HTTP_SERVICE_UNAVAILABLE, 0);
                    exchange.getResponseBody().close();
                    exchange.close();

                    return;
                }

                handlePut(exchange, resourceRequest, repository);
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

    protected void handleGet(final HttpExchange exchange, final ResourceRequest resourceRequest, final Repository repository) throws Exception {
        final ResourceResponse resourceResponse = repository.getInputStream(resourceRequest);

        if (resourceResponse == null) {
            final String message = "File not found: " + resourceRequest.getResource();
            final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(ArserUtils.HTTP_NOT_FOUND, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                exchange.getResponseBody().write(bytes);

                outputStream.flush();
            }

            return;
        }

        final long fileLength = resourceResponse.getContentLength();

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_CONTENT_TYPE, ArserUtils.getContentType(resourceResponse.getFileName()));
        exchange.sendResponseHeaders(ArserUtils.HTTP_OK, fileLength);

        try (OutputStream outputStream = new BufferedOutputStream(exchange.getResponseBody())) {
            resourceResponse.transferTo(outputStream);

            outputStream.flush();
        }
    }

    protected void handleHead(final HttpExchange exchange, final ResourceRequest resourceRequest, final Repository repository) throws Exception {
        final boolean exist = repository.exist(resourceRequest);

        final int response = exist ? ArserUtils.HTTP_OK : ArserUtils.HTTP_NOT_FOUND;

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(response, -1);
    }

    /**
     * Deploy
     **/
    protected void handlePut(final HttpExchange exchange, final ResourceRequest resourceRequest, final Repository repository) throws Exception {
        try (InputStream inputStream = new BufferedInputStream(exchange.getRequestBody())) {
            repository.write(resourceRequest, inputStream);
        }

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(ArserUtils.HTTP_OK, -1);
    }
}
