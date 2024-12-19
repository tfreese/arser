// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceHandle;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringRemoteRepositoryWebClient extends AbstractRemoteRepository {
    private final WebClient webClient;

    public SpringRemoteRepositoryWebClient(final String name, final URI uri, final WebClient webClient, final Path tempDir) {
        super(name, uri, tempDir);

        this.webClient = Objects.requireNonNull(webClient, "webClient required");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", uri);
        }

        // .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
        // .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)) // Liefert Header, Status und ResponseBody.
        // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
        return webClient.head()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                // .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
                // .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)) // Liefert Header, Status und ResponseBody.
                .exchangeToMono(clientResponse -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("exist - Response: {} / {}", clientResponse.statusCode(), uri);
                    }

                    return Mono.just(clientResponse.statusCode().is2xxSuccessful());
                })
                // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
                .blockOptional()
                .orElse(false);
    }

    @Override
    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Request: {}", uri);
        }

        return webClient.get()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchangeToMono(clientResponse -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Resource - Response: {} / {}", clientResponse.statusCode(), uri);
                    }

                    final Flux<DataBuffer> dataBufferFlux = clientResponse.body(BodyExtractors.toDataBuffers());

                    if (!clientResponse.statusCode().is2xxSuccessful()) {
                        try (OutputStream outputStream = OutputStream.nullOutputStream()) {
                            DataBufferUtils.write(dataBufferFlux, outputStream).blockLast();
                        }
                        catch (IOException ex) {
                            getLogger().error("Resource - Response: " + ex.getMessage(), ex);
                        }

                        return Mono.empty();
                    }

                    final long contentLength = clientResponse.headers().contentLength().orElse(-1);

                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
                    }

                    final ResourceHandle resourceHandle;

                    if (contentLength > 0 && contentLength < KEEP_IN_MEMORY_LIMIT) {
                        // Keep small files in Memory.
                        final byte[] bytes;

                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream((int) contentLength)) {
                            DataBufferUtils.write(dataBufferFlux, baos).blockLast();
                            baos.flush();

                            bytes = baos.toByteArray();
                        }
                        catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }

                        resourceHandle = () -> new ByteArrayInputStream(bytes);
                    }
                    else {
                        // Use Temp-Files.
                        final Path tempFile = createTempFile();

                        try (OutputStream fileOS = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             OutputStream outputStream = new BufferedOutputStream(fileOS)) {
                            DataBufferUtils.write(dataBufferFlux, outputStream).blockLast();
                            outputStream.flush();
                        }
                        catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }

                        resourceHandle = new ResourceHandle() {
                            @Override
                            public void close() {
                                try {
                                    Files.delete(tempFile);
                                }
                                catch (IOException ex) {
                                    getLogger().error(ex.getMessage(), ex);
                                }
                            }

                            @Override
                            public InputStream createInputStream() throws IOException {
                                return Files.newInputStream(tempFile);
                            }
                        };
                    }

                    return Mono.just(new DefaultResourceResponse(contentLength, resourceHandle));
                })
                .block()
                ;

        // // Variant 1: All in Memory!
        // // final Mono<ResponseEntity<InputStreamResource>> response = webClient.get()
        // //         .uri(uri)
        // //         .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
        // //         .retrieve()
        // //         .toEntity(InputStreamResource.class);
        //
        // final Mono<ResponseEntity<Flux<DataBuffer>>> response = webClient.get()
        //         .uri(uri)
        //         .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
        //         .accept(MediaType.APPLICATION_OCTET_STREAM)
        //         .retrieve()
        //         // .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
        //         .toEntityFlux(BodyExtractors.toDataBuffers())
        //         // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
        //         ;
        //
        // final ResponseEntity<Flux<DataBuffer>> responseEntity = response.block();
        // // final ResponseEntity<InputStreamResource> responseEntity = response.block();
        //
        // if (getLogger().isDebugEnabled()) {
        //     getLogger().debug("Resource - Response: {}", responseEntity);
        // }
        //
        // if (responseEntity.getStatusCode().value() != ArserUtils.HTTP_OK) {
        //     return null;
        // }
        //
        // // final InputStream inputStream = responseEntity.getBody().createInputStream();
        //
        // final Flux<DataBuffer> dataBufferFlux = responseEntity.getBody();
        //
        // // // Variant 2: All in Memory!
        // // final InputStream inputStream = dataBufferFlux
        // //         .filter(Objects::nonNull)
        // //         .map(dataBuffer -> dataBuffer.asInputStream(true))
        // //         .reduce(SequenceInputStream::new)
        // //         .block();
        // //
        // // Variant 3
        // final PipedOutputStream outputStream = new PipedOutputStream();
        // final PipedInputStream inputStream = new PipedInputStream(ArserUtils.DEFAULT_BUFFER_SIZE);
        // inputStream.connect(outputStream);
        //
        // // DataBufferUtils.write(dataBufferFlux, outputStream).blockLast();
        //
        // DataBufferUtils.write(dataBufferFlux, outputStream)
        //         .subscribeOn(Schedulers.boundedElastic())
        //         .doOnComplete(() -> {
        //             try {
        //                 outputStream.close();
        //             }
        //             catch (IOException ex) {
        //                 // Empty
        //             }
        //         })
        //         .subscribe(DataBufferUtils.releaseConsumer());
        //
        // // StreamingResponseBody stream = outputStream -> Mono.create(sink ->
        // //                 DataBufferUtils.write(dataBufferFlux, outputStream).subscribe(DataBufferUtils::release,
        // //                         sink::error,
        // //                         sink::success))
        // //         .block();
        // //
        // // return ResponseEntity.ok()
        // //         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename="+"yourFileName.pdf")
        // //         .body(stream);
        //
        // final long contentLength = responseEntity.getHeaders().getContentLength();
        //
        // if (getLogger().isDebugEnabled()) {
        //     getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
        // }
        //
        // return new DefaultResourceResponse(contentLength, () -> inputStream);
    }
}
