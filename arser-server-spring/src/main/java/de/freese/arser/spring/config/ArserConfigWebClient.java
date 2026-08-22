// Created: 21.01.24
package de.freese.arser.spring.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import de.freese.arser.repository.Repository;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryWebClient;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
@Configuration
@Profile("web-client")
public class ArserConfigWebClient extends AbstractArserConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository httpGradleLibsReleases(@Value("${arser.workingDir}") final Path workingDir, final WebClient.Builder webClientBuilder) {
        final HttpRepositoryConfig config = createGradleLibsReleasesConfig(workingDir);

        return HttpRepositoryWebClient.of(config, webClientBuilder);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository httpMavenCentral(@Value("${arser.workingDir}") final Path workingDir, final WebClient.Builder webClientBuilder) {
        final HttpRepositoryConfig config = createMavenCentralConfig(workingDir);

        return HttpRepositoryWebClient.of(config, webClientBuilder);
    }

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                ;
    }

    // @Bean
    // ClientHttpConnector clientHttpConnector() {
    //     // Doesn't work for download/get Methods!
    //     //
    //     // // Create reactor netty HTTP client.
    //     // final HttpClient httpClient = HttpClient.create()
    //     //         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100_000)
    //     //         .doOnConnected(con -> con
    //     //                 // .addHandlerLast(new ReadTimeoutHandler(100L, TimeUnit.SECONDS))
    //     //                 .addHandlerFirst(new ReadTimeoutHandler(100L, TimeUnit.SECONDS))
    //     //                 .addHandlerLast(new WriteTimeoutHandler(100L, TimeUnit.SECONDS))
    //     //         );
    //     //
    //     // // Create a client http connector using above http client.
    //     // return new ReactorClientHttpConnector(httpClient);
    //
    //     return new JdkClientHttpConnector();
    // }
    //
    // @Bean
    // WebClient webClient(final ClientHttpConnector clientHttpConnector) {
    //     return WebClient.builder()
    //             .clientConnector(clientHttpConnector)
    //             .defaultHeader(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
    //             .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
    //             .build();
    // }
    //
    // @Bean
    // CodecCustomizer codecCustomizer() {
    //     return configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024);
    // }
    //
    // @Bean
    // WebClient webClient(final WebClient.Builder builder) {
    //     return builder.build();
    // }
    //
    // @Bean
    // WebClientCustomizer webClientCustomizer(final ClientHttpConnector clientHttpConnector) {
    //     return webClientBuilder -> webClientBuilder.clientConnector(clientHttpConnector);
    // }
}
