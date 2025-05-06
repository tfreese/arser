// Created: 22.07.23
package de.freese.arser.jre.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.Arser;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.core.utils.HttpMethod;

/**
 * @author Thomas Freese
 */
public class JreHttpServerHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JreHttpServerHandler.class);

    private final Arser arser;
    private final HttpClient httpClient;

    JreHttpServerHandler(final Arser arser, final HttpClient httpClient) {
        super();

        this.arser = Objects.requireNonNull(arser, "arser required");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient required");
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

    /**
     * See Documentation of {@link HttpExchange}.
     */
    protected void consumeAndCloseRequestStream(final HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            inputStream.transferTo(OutputStream.nullOutputStream());
        }
        catch (IOException ex) {
            // Ignore
        }
    }

    protected void handleGet(final HttpExchange exchange, final ResourceRequest request, final Arser arser) throws Exception {
        final Repository repository = arser.getRepository(request.getContextRoot());

        try {
            final URI downloadUri = repository.getDownloadUri(request);

            if (downloadUri == null) {
                final String message = "HTTP-STATUS: %d for %s".formatted(ArserUtils.HTTP_STATUS_NOT_FOUND, request.getResource());
                throw new Exception(message);
            }

            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(downloadUri)
                    .GET()
                    .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                    .header("Accept", ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                    .build();

            final HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() != ArserUtils.HTTP_STATUS_OK) {
                try (InputStream inputStream = httpResponse.body()) {
                    // Drain the Body.
                    inputStream.transferTo(OutputStream.nullOutputStream());
                }

                final String message = "HTTP-STATUS: %d for %s".formatted(httpResponse.statusCode(), downloadUri.toString());
                throw new Exception(message);
            }

            final long contentLength = httpResponse.headers().firstValueAsLong(ArserUtils.HTTP_HEADER_CONTENT_LENGTH).orElse(-1);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), downloadUri);
            }

            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
            exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_CONTENT_TYPE, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM);
            exchange.sendResponseHeaders(ArserUtils.HTTP_STATUS_OK, contentLength);

            try (OutputStream outputStream = new BufferedOutputStream(exchange.getResponseBody());
                 InputStream inputStream = httpResponse.body()) {
                inputStream.transferTo(outputStream);

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

    protected void handleHead(final HttpExchange exchange, final ResourceRequest request, final Arser arser) throws Exception {
        final Repository repository = arser.getRepository(request.getContextRoot());

        final boolean exist = repository.exist(request);

        final int response = exist ? ArserUtils.HTTP_STATUS_OK : ArserUtils.HTTP_STATUS_NOT_FOUND;

        exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
        exchange.sendResponseHeaders(response, -1);
    }

    /**
     * Deploy
     **/
    protected void handlePut(final HttpExchange exchange, final ResourceRequest request, final Arser arser) throws Exception {
        final Repository repository = arser.getRepository(request.getContextRoot());

        try (InputStream inputStream = new BufferedInputStream(exchange.getRequestBody())) {
            repository.write(request, inputStream);
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
