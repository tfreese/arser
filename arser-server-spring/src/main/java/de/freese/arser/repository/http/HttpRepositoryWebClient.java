package de.freese.arser.repository.http;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.blobvalue.DefaultBlobValue;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 * @since 22.08.26
 */
public class HttpRepositoryWebClient extends AbstractHttpRepository {
    private final WebClient webClient;

    public HttpRepositoryWebClient(final HttpRepositoryConfig config, final WebClient webClient) {
        super(config);

        this.webClient = Objects.requireNonNull(webClient, "webClient required");
    }

    @Override
    public ArserResult download(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        try {
            return webClient.get()
                    .uri(remoteUri)
                    .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                    .header(ArserUtils.HTTP_HEADER_ACCEPT, ArserUtils.MIMETYPE_APPLICATION_OCTED_STREAM)
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().value() == ArserUtils.HTTP_STATUS_OK) {
                            final Flux<DataBuffer> dataBufferFlux = clientResponse.body(BodyExtractors.toDataBuffers());

                            // final Path tempFile = Path.of(System.getProperty("java.io.tmpdir")).resolve(System.nanoTime() + ArserUtils.toFileName(arserRequest.getResource()));
                            // DataBufferUtils.write(dataBufferFlux, tempFile).block();
                            // return Mono.just(new ArserResult.Download(DefaultBlobValue.of(tempFile)));

                            try (InputStream inputStream = DataBufferUtils.subscriberInputStream(dataBufferFlux, 8)) {
                                return Mono.just(new ArserResult.Download(DefaultBlobValue.of(inputStream)));
                            }
                            catch (final Exception ex) {
                                return Mono.just(new ArserResult.Failure(ex));
                            }
                        }

                        // Drain the Body.
                        clientResponse.releaseBody();

                        return Mono.just(new ArserResult.NotFound(remoteUri));
                    })
                    .doOnError(ArserResult.Failure::new)
                    .block();
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }

    @Override
    public ArserResult exist(final ArserRequest arserRequest) {
        final URI remoteUri = toRemoteUri(getUri(), arserRequest);

        try {
            // .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.error(Exception::new))
            // .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)) // Liefert Header, Status und ResponseBody.
            // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
            return webClient.head()
                    .uri(remoteUri)
                    .header(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().is2xxSuccessful()) {
                            return Mono.just(new ArserResult.Exist(remoteUri));
                        }

                        return Mono.just(new ArserResult.NotFound(remoteUri));
                    })
                    // .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(750)))
                    .blockOptional()
                    .orElse(new ArserResult.NotFound(remoteUri));
        }
        catch (final Exception ex) {
            return new ArserResult.Failure(ex);
        }
    }
}
