package de.freese.arser.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.api.Arser;
import de.freese.arser.api.ArserResult;
import de.freese.arser.api.ArserWebRequest;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.utils.ArserUtils;
import de.freese.arser.utils.HttpMethod;

/**
 * @author Thomas Freese
 * @since 22.07.23
 */
public class JreHttpServerHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JreHttpServerHandler.class);

    private final Arser arser;

    JreHttpServerHandler(final Arser arser) {
        super();

        this.arser = Objects.requireNonNull(arser, "arser required");
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

        final ArserWebRequest arserWebRequest = ArserWebRequest.of(exchange.getRequestURI());

        try {
            if (HttpMethod.HEAD.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleHead(exchange, arserWebRequest);
            } else if (HttpMethod.GET.equals(httpMethod)) {
                consumeAndCloseRequestStream(exchange);
                handleGet(exchange, arserWebRequest);
            } else if (HttpMethod.PUT.equals(httpMethod)) {
                handlePut(exchange, arserWebRequest);
            } else {
                sendError(exchange, ArserUtils.HTTP_STATUS_INTERNAL_ERROR, String.format("unknown method: %s from %s", httpMethod, exchange.getRemoteAddress()));
            }
        }
        catch (final Throwable ex) {
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
        catch (IOException _) {
            // Ignore
        }
    }

    protected void handleGet(final HttpExchange exchange, final ArserWebRequest arserWebRequest) throws Throwable {
        final ArserResult<?> arserResult = arser.download(arserWebRequest.getContextRoot(), arserWebRequest);

        if (arserResult instanceof ArserResult.Download<?>(final BlobValue blobValue)) {
            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_CONTENT_TYPE, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM);
            exchange.sendResponseHeaders(ArserUtils.HTTP_STATUS_OK, blobValue.getContentLength());

            try (blobValue;
                 OutputStream outputStream = new BufferedOutputStream(exchange.getResponseBody())) {
                blobValue.transferTo(outputStream);

                outputStream.flush();
            }
        } else if (arserResult instanceof ArserResult.Exist<?>(final URI uri, final boolean exist)) {
            final String message = "HTTP-STATUS: %d for %s".formatted(ArserUtils.HTTP_STATUS_NOT_FOUND, uri);

            sendError(exchange, ArserUtils.HTTP_STATUS_NOT_FOUND, message);
        } else if (arserResult instanceof ArserResult.Failure<?>(final Throwable cause)) {
            throw cause;
        }
    }

    protected void handleHead(final HttpExchange exchange, final ArserWebRequest arserWebRequest) throws Throwable {
        final ArserResult<?> arserResult = arser.exist(arserWebRequest.getContextRoot(), arserWebRequest);

        if (arserResult instanceof ArserResult.Exist<?>(final URI uri, final boolean exist)) {
            final int httpStatus = exist ? ArserUtils.HTTP_STATUS_OK : ArserUtils.HTTP_STATUS_NOT_FOUND;

            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
            exchange.sendResponseHeaders(httpStatus, -1);
        } else if (arserResult instanceof ArserResult.Failure<?>(final Throwable cause)) {
            throw cause;
        }
    }

    /**
     * Deploy
     **/
    protected void handlePut(final HttpExchange exchange, final ArserWebRequest arserWebRequest) throws Throwable {
        try (InputStream inputStream = new BufferedInputStream(exchange.getRequestBody())) {
            final ArserResult<?> arserResult = arser.upload(arserWebRequest.getContextRoot(), arserWebRequest, inputStream);

            if (arserResult instanceof ArserResult.Upload<?>) {
                exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
                exchange.sendResponseHeaders(ArserUtils.HTTP_STATUS_OK, -1);
            } else if (arserResult instanceof ArserResult.Failure<?>(final Throwable cause)) {
                throw cause;
            }
        }
        catch (final UnsupportedOperationException ex) {
            sendError(exchange, ArserUtils.HTTP_STATUS_FORBIDDEN, ex.getMessage());
        }
    }

    protected void sendError(final HttpExchange exchange, final int httpStatus, final String message) throws IOException {
        LOGGER.error(message);

        consumeAndCloseRequestStream(exchange);

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(httpStatus, 0);

        final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            exchange.getResponseBody().write(bytes);

            outputStream.flush();
        }

        exchange.getResponseBody().close();
        exchange.close();
    }
}
