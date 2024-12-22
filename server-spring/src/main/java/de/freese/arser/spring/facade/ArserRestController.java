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

import de.freese.arser.Arser;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResponseHandler;

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

    public ArserRestController(final Arser arser) {
        super();

        this.arser = Objects.requireNonNull(arser, "arser required");
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
    // @GetMapping
    // public ResponseEntity<StreamingResponseBody> doGet(final HttpServletRequest httpServletRequest) {
    //     // LOGGER.info("doGet: {}", httpServletRequest.getRequestURI());
    //
    //     final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());
    //
    //     final Repository repository = arser.getRepository(resourceRequest.getContextRoot());
    //
    //     if (repository == null) {
    //         return ResponseEntity.notFound().build();
    //     }
    //
    //     final StreamingResponseBody streamingResponseBody = outputStream -> {
    //         try {
    //             repository.streamTo(resourceRequest, new ResponseHandler() {
    //                 @Override
    //                 public void onError(final Exception exception) {
    //                     // Empty
    //                 }
    //
    //                 @Override
    //                 public void onSuccess(final long contentLength, final InputStream inputStream) {
    //                     try {
    //                         inputStream.transferTo(outputStream);
    //                         outputStream.flush();
    //                     }
    //                     catch (IOException ex) {
    //                         throw new UncheckedIOException(ex);
    //                     }
    //                 }
    //             });
    //         }
    //         catch (RuntimeException ex) {
    //             throw ex;
    //         }
    //         catch (IOException ex) {
    //             throw new UncheckedIOException(ex);
    //         }
    //         catch (Exception ex) {
    //             throw new RuntimeException(ex);
    //         }
    //     };
    //
    //     return ResponseEntity.ok(streamingResponseBody);
    //
    //     // return ResponseEntity.ok(new InputStreamResource(new FilterInputStream(resourceResponse.createInputStream()) {
    //     //     @Override
    //     //     public void close() throws IOException {
    //     //         super.close();
    //     //
    //     //         resourceResponse.close();
    //     //     }
    //     // }));
    //     // }
    //     // catch (Exception ex) {
    //     //     final byte[] errorMessage = ex.getMessage().getBytes(StandardCharsets.UTF_8);
    //     //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InputStreamResource(new ByteArrayInputStream(errorMessage)));
    //     // }
    // }
    @GetMapping
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final ResourceRequest resourceRequest = ResourceRequest.of(request.getRequestURI());

        response.setContentType("application/binary");

        final Repository repository = arser.getRepository(resourceRequest.getContextRoot());

        if (repository == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.flushBuffer();

            return;
        }

        try {
            repository.streamTo(resourceRequest, new ResponseHandler() {
                @Override
                public void onError(final Exception exception) throws Exception {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

                    try (OutputStream outputStream = response.getOutputStream()) {
                        outputStream.write(exception.getMessage().getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }

                @Override
                public void onSuccess(final long contentLength, final InputStream inputStream) throws Exception {
                    response.setStatus(HttpStatus.OK.value());
                    response.setContentLengthLong(contentLength);

                    try (OutputStream outputStream = response.getOutputStream()) {
                        inputStream.transferTo(outputStream);
                        outputStream.flush();
                    }
                }
            });
        }
        finally {
            response.flushBuffer();
        }
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Boolean> doHead(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doHead: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());
        final Repository repository = arser.getRepository(resourceRequest.getContextRoot());

        final boolean exist = repository.exist(resourceRequest);

        return ResponseEntity.ok(exist);
        // }
        // catch (Exception ex) {
        //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        // }
    }

    @PutMapping
    public ResponseEntity<String> doPut(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doPut: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());
        final Repository repository = arser.getRepository(resourceRequest.getContextRoot());

        try (InputStream inputStream = new BufferedInputStream(httpServletRequest.getInputStream())) {
            repository.write(resourceRequest, inputStream);
        }
        // catch (Exception ex) {
        //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        // }

        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handle(final Exception ex) {
        return ErrorResponse.builder(ex, ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)).build();
    }
}
