// Created: 19 Dez. 2024
package de.freese.arser.core.api;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;

import com.sun.net.httpserver.HttpExchange;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public final class ApiTest {

    // Reactive HttpClients won't work with spring-web!
    static void apacheHttp() throws IOException {
        final CloseableHttpClient httpClient = null;
        final HttpGet httpGet = new HttpGet("");

        final ResourceResponseTest<Void> resourceResponse = (httpStatus, contentLength, inputStream) -> null;

        httpClient.execute(httpGet, response -> {
            final int responseCode = response.getCode();

            if (responseCode != 200) {
                return resourceResponse.handleResponse(responseCode, 0L, null);
            }

            final long contentLength = response.getEntity().getContentLength();

            try (InputStream inputStream = response.getEntity().getContent()) {
                return resourceResponse.handleResponse(responseCode, contentLength, inputStream);
            }
        });
    }

    static void jreHttp() {
        final HttpExchange httpExchange = null;

        final ResourceResponseTest<Void> resourceResponse = (httpStatus, contentLength, inputStream) -> {
            if (httpStatus != 200) {
                // Error
                // final String message = "File not found: " + request.getResource();
                // final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
                final byte[] bytes = null;

                // httpExchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
                // httpExchange.sendResponseHeaders(ArserUtils.HTTP_NOT_FOUND, bytes.length);

                try (OutputStream outputStream = httpExchange.getResponseBody()) {
                    httpExchange.getResponseBody().write(bytes);

                    outputStream.flush();
                }
                catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else {
                // exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_SERVER, ArserUtils.SERVER_NAME);
                // exchange.getResponseHeaders().add(ArserUtils.HTTP_HEADER_CONTENT_TYPE, ArserUtils.getContentType(request.getFileName()));
                // exchange.sendResponseHeaders(ArserUtils.HTTP_OK, contentLength);

                try (OutputStream outputStream = new BufferedOutputStream(httpExchange.getResponseBody())) {
                    inputStream.transferTo(outputStream);

                    outputStream.flush();
                }
                catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            return null;
        };

        final HttpResponse<InputStream> httpResponse = null;

        final long contentLength = httpResponse.headers().firstValueAsLong(ArserUtils.HTTP_HEADER_CONTENT_LENGTH).orElse(-1);

        try (InputStream inputStream = httpResponse.body()) {
            resourceResponse.handleResponse(httpResponse.statusCode(), contentLength, inputStream);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    // Reactive HttpClients won't work with spring-web!
    static void restClient() {
        final RestClient restClient = null;

        final ResourceResponseTest<Void> resourceResponse = (httpStatus, contentLength, inputStream) -> null;

        restClient.get()
                .uri("")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange((clientRequest, clientResponse) -> {

                    if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                        return resourceResponse.handleResponse(clientResponse.getStatusCode().value(), 0L, null);
                    }

                    final long contentLength = clientResponse.getHeaders().getContentLength();

                    try (InputStream inputStream = clientResponse.getBody()) {
                        return resourceResponse.handleResponse(clientResponse.getStatusCode().value(), contentLength, inputStream);
                    }
                });
    }

    static ResponseEntity<InputStreamResource> spring() {
        final ResourceResponseTest<ResponseEntity> resourceResponse = (httpStatus, contentLength, inputStream) -> {
            if (httpStatus != 200) {
                // Error
                return ResponseEntity.notFound().build();
            }
            else {
                return ResponseEntity.ok(new InputStreamResource(inputStream));
            }
        };

        return null;
    }

    static void webClient() {
        final WebClient webClient = null;

        final ResourceResponseTest<Void> resourceResponse = (httpStatus, contentLength, inputStream) -> null;

        webClient.get()
                .uri("")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchangeToMono(clientResponse -> {
                    if (!clientResponse.statusCode().is2xxSuccessful()) {
                        return Mono.just(resourceResponse.handleResponse(clientResponse.statusCode().value(), 0L, null));
                    }

                    // final long contentLength = clientResponse.headers().contentLength().orElse(-1);
                    //
                    // final Flux<DataBuffer> dataBufferFlux = clientResponse.body(BodyExtractors.toDataBuffers());
                    //
                    // try (InputStream inputStream = new DataBuffersInputStream(dataBufferFlux)) {
                    //     return resourceResponse.handleResponse(clientResponse.statusCode().value(), contentLength, inputStream);
                    // }

                    return Mono.empty();
                })
                .block()
        ;
    }

    private ApiTest() {
        super();
    }
}
