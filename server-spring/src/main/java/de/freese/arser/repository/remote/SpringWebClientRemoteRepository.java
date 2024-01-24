// Created: 21.01.24
package de.freese.arser.repository.remote;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.time.Duration;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import de.freese.arser.core.repository.remote.AbstractRemoteRepository;
import de.freese.arser.core.request.ResourceRequest;
import de.freese.arser.core.response.DefaultResourceResponse;
import de.freese.arser.core.response.ResourceResponse;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class SpringWebClientRemoteRepository extends AbstractRemoteRepository {
    private final WebClient webClient;

    public SpringWebClientRemoteRepository(final String name, final URI uri, final WebClient webClient) {
        super(name, uri);

        this.webClient = assertNotNull(webClient, () -> "webClient");
    }

    @Override
    protected boolean doExist(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        // @formatter:off
        final Mono<ResponseEntity<Boolean>> response = webClient.head()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(Boolean.class)) // Liefert Header, Status und ResponseBody.
                .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
                ;
        // @formatter:on

        final ResponseEntity<Boolean> responseEntity = response.block();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("exist - Response: {}", responseEntity);
        }

        assert responseEntity != null;

        return responseEntity.getStatusCode().value() == ArserUtils.HTTP_OK;
    }

    @Override
    protected ResourceResponse doGetInputStream(final ResourceRequest request) throws Exception {
        final URI uri = createResourceUri(getUri(), request.getResource());

        // @formatter:off
        final Mono<ResponseEntity<Flux<DataBuffer>>> response = webClient.get()
                .uri(uri)
                .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .retrieve()
//                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
                .toEntityFlux(BodyExtractors.toDataBuffers())
                .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
                ;
        // @formatter:on

        final ResponseEntity<Flux<DataBuffer>> responseEntity = response.block();

        assert responseEntity != null;

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("getInputStream - Response: {}", responseEntity);
        }

        if (responseEntity.getStatusCode().value() != ArserUtils.HTTP_OK) {
            return null;
        }

        final Flux<DataBuffer> dataBufferFlux = responseEntity.getBody();

        if (dataBufferFlux == null) {
            return null;
        }

        // Variant 1
        // @formatter:off
//        final InputStream inputStream = dataBufferFlux
//                .filter(Objects::nonNull)
//                .map(dataBuffer -> dataBuffer.asInputStream(true))
//                .reduce(SequenceInputStream::new)
//                .block()
//                ;
        // @formatter:on

        // Variant 2
        final PipedOutputStream outputStream = new PipedOutputStream();
        final PipedInputStream inputStream = new PipedInputStream(1024 * 10);
        inputStream.connect(outputStream);

        DataBufferUtils.write(dataBufferFlux, outputStream).subscribeOn(Schedulers.boundedElastic()).doOnComplete(() -> {
            try {
                outputStream.close();
            }
            catch (IOException ex) {
                // Empty
            }
        }).subscribe(DataBufferUtils.releaseConsumer());

        final String contentLengthString = responseEntity.getHeaders().getFirst(ArserUtils.HTTP_HEADER_CONTENT_LENGTH);
        final long contentLength = contentLengthString == null ? 0 : Long.parseLong(contentLengthString);

        return new DefaultResourceResponse(request, contentLength, inputStream);
    }
}
