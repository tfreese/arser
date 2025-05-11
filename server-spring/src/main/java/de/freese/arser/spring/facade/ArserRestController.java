// Created: 21.01.24
package de.freese.arser.spring.facade;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.freese.arser.core.model.FileResource;
import de.freese.arser.core.model.ResourceRequest;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.instance.ArserInstance;

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
    private final ArserInstance arserInstance;

    public ArserRestController(final ArserInstance arserInstance) {
        super();

        this.arserInstance = Objects.requireNonNull(arserInstance, "arserInstance required");
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

        final Repository repository = arserInstance.getRepository(resourceRequest.getContextRoot());

        if (repository == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.flushBuffer();

            return;
        }

        try {
            final FileResource fileResource = repository.getResource(resourceRequest);

            if (fileResource == null) {
                final String message = "HTTP-STATUS: %d for %s".formatted(ArserUtils.HTTP_STATUS_NOT_FOUND, resourceRequest.getResource());
                response.setStatus(HttpStatus.NOT_FOUND.value());

                try (OutputStream outputStream = response.getOutputStream()) {
                    outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }

                return;
            }

            final long contentLength = fileResource.getContentLength();
            response.setStatus(HttpStatus.OK.value());
            response.setContentLengthLong(contentLength);

            try (OutputStream outputStream = response.getOutputStream()) {
                fileResource.transferTo(outputStream);
                outputStream.flush();
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
        final Repository repository = arserInstance.getRepository(resourceRequest.getContextRoot());

        final boolean exist = repository.exist(resourceRequest);

        return exist ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping
    public ResponseEntity<String> doPut(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doPut: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());
        final Repository repository = arserInstance.getRepository(resourceRequest.getContextRoot());

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
