// Created: 21.01.24
package de.freese.arser.spring.facade;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.freese.arser.core.Arser;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.ResourceResponse;

/**
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
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Boolean> doHead(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doHead: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());

        final boolean exist = arser.exist(resourceRequest);

        return ResponseEntity.ok(exist);
    }

    @PutMapping
    public ResponseEntity<Void> doPut(final HttpServletRequest httpServletRequest) throws Exception {
        // LOGGER.info("doPut: {}", httpServletRequest.getRequestURI());

        final ResourceRequest resourceRequest = ResourceRequest.of(httpServletRequest.getRequestURI());

        try (InputStream inputStream = new BufferedInputStream(httpServletRequest.getInputStream())) {
            arser.write(resourceRequest, inputStream);
        }

        return ResponseEntity.ok().build();
    }
}
