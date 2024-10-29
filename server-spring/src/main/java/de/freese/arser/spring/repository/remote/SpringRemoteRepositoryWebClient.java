// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import de.freese.arser.core.repository.remote.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringRemoteRepositoryWebClient extends AbstractRemoteRepository {
    private final WebClient webClient;

    public SpringRemoteRepositoryWebClient(final String name, final URI uri, final WebClient webClient) {
        super(name, uri, null);

        this.webClient = assertNotNull(webClient, () -> "webClient");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Request: {}", uri);
        }

        final Mono<ResponseEntity<String>> response = webClient.head()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)) // Liefert Header, Status und ResponseBody.
                // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
                ;

        final ResponseEntity<String> responseEntity = response.block();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {}", responseEntity);
        }

        assert responseEntity != null;

        return responseEntity.getStatusCode().value() == ArserUtils.HTTP_OK;
    }

    @Override
    protected ResourceResponse doGetResource(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Request: {}", uri);
        }

        // Variant 1: All in Memory!
        // final Mono<ResponseEntity<InputStreamResource>> response = webClient.get()
        //         .uri(uri)
        //         .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
        //         .retrieve()
        //         .toEntity(InputStreamResource.class);

        final Mono<ResponseEntity<Flux<DataBuffer>>> response = webClient.get()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .retrieve()
                // .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
                .toEntityFlux(BodyExtractors.toDataBuffers())
                // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
                ;

        final ResponseEntity<Flux<DataBuffer>> responseEntity = response.block();
        // final ResponseEntity<InputStreamResource> responseEntity = response.block();

        assert responseEntity != null;

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Resource - Response: {}", responseEntity);
        }

        if (responseEntity.getStatusCode().value() != ArserUtils.HTTP_OK) {
            return null;
        }

        // final InputStream inputStream = responseEntity.getBody().createInputStream();

        final Flux<DataBuffer> dataBufferFlux = responseEntity.getBody();

        // // Variant 2: All in Memory!
        // final InputStream inputStream = dataBufferFlux
        //         .filter(Objects::nonNull)
        //         .map(dataBuffer -> dataBuffer.asInputStream(true))
        //         .reduce(SequenceInputStream::new)
        //         .block();
        //
        // Variant 3
        final PipedOutputStream outputStream = new PipedOutputStream();
        final PipedInputStream inputStream = new PipedInputStream(ArserUtils.DEFAULT_BUFFER_SIZE);
        inputStream.connect(outputStream);

        // DataBufferUtils.write(dataBufferFlux, outputStream).blockLast();

        DataBufferUtils.write(dataBufferFlux, outputStream)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnComplete(() -> {
                    try {
                        outputStream.close();
                    }
                    catch (IOException ex) {
                        // Empty
                    }
                })
                .subscribe(DataBufferUtils.releaseConsumer());

        // StreamingResponseBody stream = outputStream -> Mono.create(sink ->
        //                 DataBufferUtils.write(dataBufferFlux, outputStream).subscribe(DataBufferUtils::release,
        //                         sink::error,
        //                         sink::success))
        //         .block();
        //
        // return ResponseEntity.ok()
        //         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename="+"yourfilename.pdf")
        //         .body(stream);

        final long contentLength = responseEntity.getHeaders().getContentLength();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Download {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
        }

        return new DefaultResourceResponse(contentLength, () -> inputStream);
    }
}
