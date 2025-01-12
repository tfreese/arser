// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.net.URI;
import java.util.Objects;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import de.freese.arser.core.repository.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
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
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI remoteUri = createRemoteUri(getBaseUri(), request.getResource());

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
}
