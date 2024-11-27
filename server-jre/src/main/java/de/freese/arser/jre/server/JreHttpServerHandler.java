// Created: 22.07.23
package de.freese.arser.jre.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.freese.arser.core.Arser;
import de.freese.arser.core.component.AbstractComponent;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.core.utils.HttpMethod;

/**
 * @author Thomas Freese
 */
public class JreHttpServerHandler extends AbstractComponent implements HttpHandler {
    private final Arser arser;

    JreHttpServerHandler(final Arser arser) {
        super();

        this.arser = Objects.requireNonNull(arser, "arser required");
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

        final ResourceRequest request = ResourceRequest.of(exchange.getRequestURI());

        try {
            if (HttpMethod.GET.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleGet(exchange, request, arser);
            }
            else if (HttpMethod.HEAD.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleHead(exchange, request, arser);
            }
            else if (HttpMethod.PUT.equals(httpMethod)) {
                handlePut(exchange, request, arser);
            }
            else {
                sendError(exchange, String.format("unknown method: %s from %s", httpMethod, exchange.getRemoteAddress()));
            }
        }
        catch (final IOException ex) {
            getLogger().error(ex.getMessage(), ex);
        }
        catch (final Exception ex) {
            sendError(exchange, ex.getMessage());
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

    protected void handleGet(final HttpExchange exchange, final ResourceRequest request, final Arser arser) throws Exception {
        try (ResourceResponse response = arser.getResource(request)) {
            if (response == null) {
                final String message = "File not found: " + request.getResource();
                final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
                exchange.sendResponseHeaders(ArserUtils.HTTP_NOT_FOUND, bytes.length);

                try (OutputStream outputStream = exchange.getResponseBody()) {
                    exchange.getResponseBody().write(bytes);

                    outputStream.flush();
                }

                return;
            }

            final long contentLength = response.getContentLength();

            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_CONTENT_TYPE, ArserUtils.getContentType(request.getFileName()));
            exchange.sendResponseHeaders(ArserUtils.HTTP_OK, contentLength);

            try (OutputStream outputStream = new BufferedOutputStream(exchange.getResponseBody())) {
                response.transferTo(outputStream);

                outputStream.flush();
            }
        }
    }

    protected void handleHead(final HttpExchange exchange, final ResourceRequest request, final Arser arser) throws Exception {
        final boolean exist = arser.exist(request);

        final int response = exist ? ArserUtils.HTTP_OK : ArserUtils.HTTP_NOT_FOUND;

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(response, -1);
    }

    /**
     * Deploy
     **/
    protected void handlePut(final HttpExchange exchange, final ResourceRequest request, final Arser arser) throws Exception {
        try (InputStream inputStream = new BufferedInputStream(exchange.getRequestBody())) {
            arser.write(request, inputStream);
        }
        catch (Exception ex) {
            sendError(exchange, ex.getMessage());

            return;
        }

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(ArserUtils.HTTP_OK, -1);
    }

    protected void sendError(final HttpExchange exchange, final String message) throws IOException {
        getLogger().error(message);

        consumeAndCloseRequestStream(exchange);

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(ArserUtils.HTTP_INTERNAL_ERROR, 0);
        exchange.getResponseBody().close();
        exchange.close();
    }
}
