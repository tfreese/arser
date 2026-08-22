// Created: 21.01.24
package de.freese.arser.spring.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

import de.freese.arser.repository.Repository;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryRestClient;
import de.freese.arser.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
@Configuration
@Profile("rest-client")
public class ArserConfigRestClient extends AbstractArserConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository httpGradleLibsReleases(@Value("${arser.workingDir}") final Path workingDir, final RestClient.Builder restClientBuilder) {
        final HttpRepositoryConfig config = createGradleLibsReleasesConfig(workingDir);

        return HttpRepositoryRestClient.of(config, restClientBuilder);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository httpMavenCentral(@Value("${arser.workingDir}") final Path workingDir, final RestClient.Builder restClientBuilder) {
        final HttpRepositoryConfig config = createMavenCentralConfig(workingDir);

        return HttpRepositoryRestClient.of(config, restClientBuilder);
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .defaultHeader(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                ;
    }
    //
    // @Bean
    // ClientHttpRequestFactory clientHttpRequestFactory() {
    //     return new JdkClientHttpRequestFactory();
    // }
    //
    // @Bean
    // RestClient restClient(final ClientHttpRequestFactory clientHttpRequestFactory) {
    //     return RestClient.builder()
    //             .requestFactory(clientHttpRequestFactory)
    //             .defaultHeader(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
    //             .build();
    // }
    //
    // @Bean
    // RestClientCustomizer restClientCustomizer(final ClientHttpRequestFactory clientHttpRequestFactory) {
    //     return restClientBuilder -> restClientBuilder.requestFactory(clientHttpRequestFactory);
    // }
}
