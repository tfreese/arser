// Created: 21.01.24
package de.freese.arser.spring.facade;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.InputStreamResource;
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

import de.freese.arser.core.Arser;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;

/**
 * <a href="https://dev.to/rpkr/different-ways-to-send-a-file-as-a-response-in-spring-boot-for-a-rest-api-43g7">different-ways-to-send-a-file</a>
 *
 * @author Thomas Freese
 */
@RestController
@RequestMapping(path = "**")
public class ArserRestController {
    // private static final Logger LOGGER = LoggerFactory.getLogger(ArserRestController.class);

    @Resource
    private Arser arser;

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
    public ResponseEntity<InputStreamResource> doGet(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doGet: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());

        // StreamingResponseBody streamingResponseBody=outputStream -> {
        //
        // };

        // try {
        final ResourceResponse resourceResponse = arser.getResource(resourceRequest);

        if (resourceResponse == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new InputStreamResource(new FilterInputStream(resourceResponse.createInputStream()) {
            @Override
            public void close() throws IOException {
                super.close();

                resourceResponse.close();
            }
        }));
        // }
        // catch (Exception ex) {
        //     final byte[] errorMessage = ex.getMessage().getBytes(StandardCharsets.UTF_8);
        //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InputStreamResource(new ByteArrayInputStream(errorMessage)));
        // }
    }

    // public void doGett(final HttpServletRequest request, final HttpServletResponse response) {
    //     final ResourceRequest resourceRequest = ResourceRequest.of(request.getRequestURI());
    //
    //     response.setContentType("application/binary");
    //
    //     final ResourceResponse resourceResponse = arser.getResource(resourceRequest);
    //
    //     if (resourceResponse == null) {
    //         response.setStatus(HttpStatus.NOT_FOUND.value());
    //         response.flushBuffer();
    //         return;
    //     }
    //
    //     try (OutputStream outputStream = response.getOutputStream()) {
    //         resourceResponse.transferTo(outputStream);
    //         outputStream.flush();
    //
    //         response.setStatus(HttpStatus.OK.value());
    //         response.flushBuffer();
    //     }
    //     catch (IOException e) {
    //         response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    //     }
    // }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Boolean> doHead(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doHead: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());

        // try {
        final boolean exist = arser.exist(resourceRequest);

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

        try (InputStream inputStream = new BufferedInputStream(httpServletRequest.getInputStream())) {
            arser.write(resourceRequest, inputStream);
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
