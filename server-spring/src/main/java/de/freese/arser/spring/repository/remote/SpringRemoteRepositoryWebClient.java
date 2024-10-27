// Created: 21.01.24
package de.freese.arser.spring.repository.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import de.freese.arser.core.repository.remote.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceInfo;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringRemoteRepositoryWebClient extends AbstractRemoteRepository {
    private final WebClient webClient;

    public SpringRemoteRepositoryWebClient(final String name, final URI uri, final WebClient webClient) {
        super(name, uri);

        this.webClient = assertNotNull(webClient, () -> "webClient");
    }

    @Override
    protected ResourceInfo doConsume(final ResourceRequest request, final OutputStream outputStream) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        final Mono<ResponseEntity<Flux<DataBuffer>>> response = webClient.get()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .retrieve()
                .toEntityFlux(BodyExtractors.toDataBuffers());

        final ResponseEntity<Flux<DataBuffer>> responseEntity = response.block();

        assert responseEntity != null;

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - Response: {}", responseEntity);
        }

        if (responseEntity.getStatusCode().value() != ArserUtils.HTTP_OK) {
            return null;
        }

        final Flux<DataBuffer> dataBufferFlux = responseEntity.getBody();

        dataBufferFlux.subscribe(dataBuffer -> {
            try (InputStream inputStream = dataBuffer.asInputStream(true)) {
                inputStream.transferTo(outputStream);
            }
            catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        final String contentLengthString = responseEntity.getHeaders().getFirst(ArserUtils.HTTP_HEADER_CONTENT_LENGTH);
        final long contentLength = contentLengthString == null ? 0 : Long.parseLong(contentLengthString);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Downloaded {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
        }

        return new ResourceInfo(request, contentLength);
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

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
    protected ResourceResponse doGetInputStream(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        // Variant 1: All in Memory!
        final Mono<ResponseEntity<InputStreamResource>> response = webClient.get()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .retrieve()
                .toEntity(InputStreamResource.class);
        // final ResponseEntity<InputStreamResource> re = response.block();
        // final InputStreamResource isr = re.getBody();
        // final InputStream is = isr.getInputStream();
        // if (getLogger().isDebugEnabled()) {
        //     getLogger().debug("getInputStream - Available: {}", is.available());
        // }
        // is.close();

        // final Mono<ResponseEntity<Flux<DataBuffer>>> response = webClient.get()
        //         .uri(uri)
        //         .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
        //         .retrieve()
        //         // .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
        //         .toEntityFlux(BodyExtractors.toDataBuffers())
        //         // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
        //         ;

        // final ResponseEntity<Flux<DataBuffer>> responseEntity = response.block();
        final ResponseEntity<InputStreamResource> responseEntity = response.block();

        assert responseEntity != null;

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - Response: {}", responseEntity);
        }

        if (responseEntity.getStatusCode().value() != ArserUtils.HTTP_OK) {
            return null;
        }

        final InputStream inputStream = responseEntity.getBody().getInputStream();

        // final Flux<DataBuffer> dataBufferFlux = responseEntity.getBody();
        //
        // // Variant 2: All in Memory!
        // final InputStream inputStream = dataBufferFlux
        //         .filter(Objects::nonNull)
        //         .map(dataBuffer -> dataBuffer.asInputStream(true))
        //         .reduce(SequenceInputStream::new)
        //         .block();
        //
        // Variant 3
        // final PipedOutputStream outputStream = new PipedOutputStream();
        // final PipedInputStream inputStream = new PipedInputStream(ArserUtils.DEFAULT_BUFFER_SIZE);
        // inputStream.connect(outputStream);
        //
        // DataBufferUtils.write(dataBufferFlux, outputStream).subscribeOn(Schedulers.boundedElastic()).doOnComplete(() -> {
        //     try {
        //         outputStream.close();
        //     }
        //     catch (IOException ex) {
        //         // Empty
        //     }
        // }).subscribe(DataBufferUtils.releaseConsumer());

        final String contentLengthString = responseEntity.getHeaders().getFirst(ArserUtils.HTTP_HEADER_CONTENT_LENGTH);
        final long contentLength = contentLengthString == null ? 0 : Long.parseLong(contentLengthString);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Downloaded {} Bytes [{}]: {} ", contentLength, ArserUtils.toHumanReadable(contentLength), uri);
        }

        return new DefaultResourceResponse(request, contentLength, inputStream);
    }
}
