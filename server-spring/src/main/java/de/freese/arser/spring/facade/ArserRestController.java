// Created: 21.01.24
package de.freese.arser.spring.facade;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.freese.arser.Arser;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.utils.ArserUtils;

/**
 * <a href="https://dev.to/rpkr/different-ways-to-send-a-file-as-a-response-in-spring-boot-for-a-rest-api-43g7">different-ways-to-send-a-file</a>
 *
 * @author Thomas Freese
 */
@RestController
@RequestMapping(path = "**")
public class ArserRestController {
    // private static final Logger LOGGER = LoggerFactory.getLogger(ArserRestController.class);

    // @Resource
    private final Arser arser;
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    public ArserRestController(final Arser arser, final ClientHttpRequestFactory clientHttpRequestFactory) {
        super();

        this.arser = Objects.requireNonNull(arser, "arser required");
        this.clientHttpRequestFactory = Objects.requireNonNull(clientHttpRequestFactory, "clientHttpRequestFactory required");
    }

    /**
     * Jakarta:<br>
     * <pre>{@code
     * public Response test(@PathVariable("id") final UUID id) throws IOException {
     *     return Response.ok((StreamingOutput) outputStream -> {
     *              try (InputStream inputStream = new … {
     *                 inputStream.transferTo(outputStream);
     *                 outputStream.flush();
     *             }
     *         }).build();
     * }     * }</pre>
     *
     * StreamingResponseBody, InputStreamResource working booth alone and with ResponseEntity.
     */
    @GetMapping
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final ResourceRequest resourceRequest = ResourceRequest.of(request.getRequestURI());

        // response.setContentType("application/binary");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        final Repository repository = arser.getRepository(resourceRequest.getContextRoot());

        if (repository == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.flushBuffer();

            return;
        }

        try {
            final URI downloadUri = repository.getDownloadUri(resourceRequest);

            if (downloadUri == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());

                try (OutputStream outputStream = response.getOutputStream()) {
                    final String message = "HTTP-STATUS: %d for %s".formatted(HttpStatus.NOT_FOUND.value(), resourceRequest.getResource());
                    outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }

                return;
            }

            final ClientHttpRequest clientHttpRequest = clientHttpRequestFactory.createRequest(downloadUri, HttpMethod.GET);
            clientHttpRequest.getHeaders().put(ArserUtils.HTTP_HEADER_USER_AGENT, List.of(ArserUtils.SERVER_NAME));
            clientHttpRequest.getHeaders().put("Accept", List.of(MediaType.APPLICATION_OCTET_STREAM_VALUE));

            try (ClientHttpResponse clientHttpResponse = clientHttpRequest.execute()) {
                final int responseCode = clientHttpResponse.getStatusCode().value();

                if (responseCode != ArserUtils.HTTP_STATUS_OK) {
                    // Drain Body.
                    try (InputStream inputStream = clientHttpResponse.getBody()) {
                        inputStream.transferTo(OutputStream.nullOutputStream());
                    }

                    response.setStatus(HttpStatus.NOT_FOUND.value());

                    try (OutputStream outputStream = response.getOutputStream()) {
                        final String message = "HTTP-STATUS: %d for %s".formatted(responseCode, downloadUri.toString());
                        outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }

                    return;
                }

                final long contentLength = clientHttpResponse.getHeaders().getContentLength();
                response.setStatus(HttpStatus.OK.value());
                response.setContentLengthLong(contentLength);

                try (InputStream inputStream = clientHttpResponse.getBody();
                     OutputStream outputStream = response.getOutputStream()) {
                    inputStream.transferTo(outputStream);
                    outputStream.flush();
                }
            }
        }
        finally {
            response.flushBuffer();
        }
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> doHead(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doHead: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());
        final Repository repository = arser.getRepository(resourceRequest.getContextRoot());

        final boolean exist = repository.exist(resourceRequest);

        return exist ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping
    public ResponseEntity<String> doPut(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doPut: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());
        final Repository repository = arser.getRepository(resourceRequest.getContextRoot());

        try (InputStream inputStream = new BufferedInputStream(httpServletRequest.getInputStream())) {
            repository.write(resourceRequest, inputStream);

            return ResponseEntity.ok().build();
        }
        catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handle(final Exception ex) {
        return ErrorResponse.builder(ex, ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)).build();
    }
}
