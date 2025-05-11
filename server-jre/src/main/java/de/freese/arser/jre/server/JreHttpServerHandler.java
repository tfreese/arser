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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.core.model.FileResource;
import de.freese.arser.core.model.ResourceRequest;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.core.utils.HttpMethod;
import de.freese.arser.instance.ArserInstance;

/**
 * @author Thomas Freese
 */
public class JreHttpServerHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JreHttpServerHandler.class);

    private final ArserInstance arserInstance;

    JreHttpServerHandler(final ArserInstance arserInstance) {
        super();

        this.arserInstance = Objects.requireNonNull(arserInstance, "arserInstance required");
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        final HttpMethod httpMethod = HttpMethod.get(exchange.getRequestMethod());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}: {}", httpMethod, exchange.getRequestURI());

            if (LOGGER.isTraceEnabled()) {
                exchange.getRequestHeaders().forEach((key, value) -> LOGGER.trace("{} = {}", key, value));
            }
        }

        final ResourceRequest resourceRequest = ResourceRequest.of(exchange.getRequestURI());

        try {
            if (HttpMethod.HEAD.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleHead(exchange, resourceRequest, arserInstance);
            }
            else if (HttpMethod.GET.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleGet(exchange, resourceRequest, arserInstance);
            }
            else if (HttpMethod.PUT.equals(httpMethod)) {
                handlePut(exchange, resourceRequest, arserInstance);
            }
            else {
                sendError(exchange, ArserUtils.HTTP_STATUS_INTERNAL_ERROR, String.format("unknown method: %s from %s", httpMethod, exchange.getRemoteAddress()));
            }
        }
        catch (final Exception ex) {
            LOGGER.error(ex.getMessage(), ex);

            sendError(exchange, ArserUtils.HTTP_STATUS_INTERNAL_ERROR, ex.getMessage());
        }
        finally {
            exchange.getResponseBody().close();
            exchange.close();
        }
    }

    protected void consumeAndClose(final InputStream inputStream) throws IOException {
        // Drain the Body.
        inputStream.transferTo(OutputStream.nullOutputStream());
        inputStream.close();
    }

    /**
     * See Documentation of {@link HttpExchange}.
     */
    protected void consumeAndCloseRequestStream(final HttpExchange exchange) {
        try (InputStream inputStream = exchange.getRequestBody()) {
            consumeAndClose(inputStream);
        }
        catch (IOException ex) {
            // Ignore
        }
    }

    protected void handleGet(final HttpExchange exchange, final ResourceRequest resourceRequest, final ArserInstance arserInstance) throws Exception {
        final Repository repository = arserInstance.getRepository(resourceRequest.getContextRoot());

        try {
            final FileResource fileResource = repository.getResource(resourceRequest);

            if (fileResource == null) {
                final String message = "HTTP-STATUS: %d for %s".formatted(ArserUtils.HTTP_STATUS_NOT_FOUND, resourceRequest.getResource());
                throw new Exception(message);
            }

            final long contentLength = fileResource.getContentLength();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), resourceRequest.getResource());
            }

            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_CONTENT_TYPE, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM);
            exchange.sendResponseHeaders(ArserUtils.HTTP_STATUS_OK, contentLength);

            try (OutputStream outputStream = new BufferedOutputStream(exchange.getResponseBody())) {
                fileResource.transferTo(outputStream);

                outputStream.flush();
            }
        }
        catch (Exception ex) {
            final byte[] bytes = ex.getMessage().getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
            exchange.sendResponseHeaders(ArserUtils.HTTP_STATUS_NOT_FOUND, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                exchange.getResponseBody().write(bytes);

                outputStream.flush();
            }
        }
    }

    protected void handleHead(final HttpExchange exchange, final ResourceRequest resourceRequest, final ArserInstance arserInstance) throws Exception {
        final Repository repository = arserInstance.getRepository(resourceRequest.getContextRoot());

        final boolean exist = repository.exist(resourceRequest);

        final int response = exist ? ArserUtils.HTTP_STATUS_OK : ArserUtils.HTTP_STATUS_NOT_FOUND;

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(response, -1);
    }

    /**
     * Deploy
     **/
    protected void handlePut(final HttpExchange exchange, final ResourceRequest resourceRequest, final ArserInstance arserInstance) throws Exception {
        final Repository repository = arserInstance.getRepository(resourceRequest.getContextRoot());

        try (InputStream inputStream = new BufferedInputStream(exchange.getRequestBody())) {
            repository.write(resourceRequest, inputStream);
        }
        catch (Exception ex) {
            sendError(exchange, ArserUtils.HTTP_STATUS_FORBIDDEN, ex.getMessage());

            return;
        }

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(ArserUtils.HTTP_STATUS_OK, -1);
    }

    protected void sendError(final HttpExchange exchange, final int httpStatus, final String message) throws IOException {
        LOGGER.error(message);

        consumeAndCloseRequestStream(exchange);

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(httpStatus, 0);
        exchange.getResponseBody().close();
        exchange.close();
    }
}
