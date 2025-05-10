// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import de.freese.arser.core.model.DefaultFileResource;
import de.freese.arser.core.model.FileResource;
import de.freese.arser.core.model.ResourceRequest;
import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class RemoteRepositoryWebClient extends AbstractRemoteRepository {
    private final WebClient webClient;

    public RemoteRepositoryWebClient(final String name, final URI baseUri, final WebClient webClient) {
        super(name, baseUri);

        this.webClient = Objects.requireNonNull(webClient, "webClient required");
    }

    @Override
    protected boolean doExist(final ResourceRequest resourceRequest) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), resourceRequest.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", remoteUri);
        }

        // .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
        // .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)) // Liefert Header, Status und ResponseBody.
        // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
        return webClient.head()
                .uri(remoteUri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                // .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
                // .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)) // Liefert Header, Status und ResponseBody.
                .exchangeToMono(clientResponse -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("exist - Response: {} / {}", clientResponse.statusCode(), remoteUri);
                    }

                    return Mono.just(clientResponse.statusCode().is2xxSuccessful());
                })
                // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
                .blockOptional()
                .orElse(false);
    }

    @Override
    protected FileResource doGetResource(final ResourceRequest resourceRequest) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), resourceRequest.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("RequestResource: {}", remoteUri);
        }

        return webClient.get()
                .uri(remoteUri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .header(ArserUtils.HTTP_HEADER_ACCEPT, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                .exchangeToMono(clientResponse -> {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().warn("HTTP-STATUS: {} for {}", clientResponse.statusCode().value(), remoteUri);
                    }

                    if (clientResponse.statusCode().value() != ArserUtils.HTTP_STATUS_OK) {
                        clientResponse.releaseBody();
                        // try (InputStream inputStream = clientResponse.body(BodyExtractors.)) {
                        //     // Drain the Body.
                        //     inputStream.transferTo(OutputStream.nullOutputStream());
                        // }

                        return null;
                    }

                    final long contentLength = clientResponse.headers().contentLength().orElse(-1L);
                    final Path path = getWorkingDir().resolve(System.nanoTime() + ArserUtils.toFileName(resourceRequest.getResource()));

                    final Flux<DataBuffer> dataBufferFlux = clientResponse.body(BodyExtractors.toDataBuffers());

                    DataBufferUtils.write(dataBufferFlux, path).block();

                    return Mono.just(new DefaultFileResource(contentLength, () ->
                            new FilterInputStream(Files.newInputStream(path)) {
                                @Override
                                public void close() throws IOException {
                                    super.close();

                                    // Delete Temp-File.
                                    try {
                                        Files.delete(path);
                                    }
                                    catch (IOException ex) {
                                        getLogger().error(ex.getMessage(), ex);
                                    }
                                }
                            }));
                })
                .block();
    }
}

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
