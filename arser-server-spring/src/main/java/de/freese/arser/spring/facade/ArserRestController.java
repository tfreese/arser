package de.freese.arser.spring.facade;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
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

import de.freese.arser.api.Arser;
import de.freese.arser.api.ArserResult;
import de.freese.arser.api.ArserWebRequest;
import de.freese.arser.blobvalue.BlobValue;

/**
 * <a href="https://dev.to/rpkr/different-ways-to-send-a-file-as-a-response-in-spring-boot-for-a-rest-api-43g7">different-ways-to-send-a-file</a>
 *
 * @author Thomas Freese
 * @since 21.01.24
 */
@RestController
@RequestMapping(path = "**")
public class ArserRestController {
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
    @GetMapping
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final ArserWebRequest arserWebRequest = ArserWebRequest.of(request.getRequestURI());
        final ArserResult arserResult = arser.download(arserWebRequest.getContextRoot(), arserWebRequest);

        if (arserResult instanceof ArserResult.Download(final BlobValue blobValue)) {
            response.addHeader(de.freese.arser.utils.ArserUtils.HTTP_HEADER_SERVER, de.freese.arser.utils.ArserUtils.SERVER_NAME);
            response.addHeader(de.freese.arser.utils.ArserUtils.HTTP_HEADER_CONTENT_TYPE, de.freese.arser.utils.ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM);
            response.setStatus(HttpStatus.OK.value());
            response.setContentLength((int) blobValue.getContentLength());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            try (blobValue;
                 OutputStream outputStream = new BufferedOutputStream(response.getOutputStream())) {
                blobValue.transferTo(outputStream);

                outputStream.flush();
            }

            response.flushBuffer();
        }
        else if (arserResult instanceof ArserResult.NotFound(final URI uri)) {
            sendResponse(response, HttpStatus.NOT_FOUND, uri.toString());
        }
        else if (arserResult instanceof final ArserResult.Forbidden fb) {
            sendResponse(response, HttpStatus.FORBIDDEN, fb.reason());
        }
        else if (arserResult instanceof ArserResult.Failure(final Throwable cause)) {
            sendResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, cause.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> doHead(final HttpServletRequest httpServletRequest) {
        final ArserWebRequest arserWebRequest = ArserWebRequest.of(httpServletRequest.getRequestURI());
        final ArserResult arserResult = arser.download(arserWebRequest.getContextRoot(), arserWebRequest);

        if (arserResult instanceof ArserResult.Exist) {
            return ResponseEntity.ok().build();
        }
        else if (arserResult instanceof final ArserResult.Forbidden fb) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).header("REASON", fb.reason()).build();
        }
        else if (arserResult instanceof ArserResult.Failure(final Throwable cause)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header("FAILIURE", cause.getMessage()).build();
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping
    public ResponseEntity<String> doPut(final HttpServletRequest httpServletRequest) {
        final ArserWebRequest arserWebRequest = ArserWebRequest.of(httpServletRequest.getRequestURI());

        try (InputStream inputStream = new BufferedInputStream(httpServletRequest.getInputStream())) {
            final ArserResult arserResult = arser.upload(arserWebRequest.getContextRoot(), arserWebRequest, inputStream);

            if (arserResult instanceof ArserResult.Upload) {
                return ResponseEntity.ok().build();
            }
            else if (arserResult instanceof final ArserResult.Forbidden fb) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(fb.reason());
            }
            else if (arserResult instanceof ArserResult.Failure(final Throwable cause)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cause.getMessage());
            }
        }
        catch (final Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handle(final Exception ex) {
        return ErrorResponse.builder(ex, ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)).build();
    }

    private void sendResponse(final HttpServletResponse response, final HttpStatus httpStatus, final String message) throws IOException {
        response.setStatus(httpStatus.value());

        response.setHeader(de.freese.arser.utils.ArserUtils.HTTP_HEADER_SERVER, de.freese.arser.utils.ArserUtils.SERVER_NAME);

        final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);

        try (OutputStream outputStream = response.getOutputStream()) {
            outputStream.write(bytes);
            outputStream.flush();
        }

        response.flushBuffer();
    }
}
