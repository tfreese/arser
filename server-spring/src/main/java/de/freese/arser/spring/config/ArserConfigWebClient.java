// Created: 21.01.24
package de.freese.arser.spring.config;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import de.freese.arser.config.ArserConfig;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.instance.ArserInstance;
import de.freese.arser.spring.SpringArserInstance;
import de.freese.arser.spring.repository.remote.RemoteRepositoryWebClient;

/**
 * @author Thomas Freese
 */
@Configuration
@Profile("web-client")
public class ArserConfigWebClient {
    @Bean(destroyMethod = "shutdown")
    ArserInstance arserInstance(@Value("${arser.workingDir}") final Path workingDir) {
        return new SpringArserInstance("arserInstance_spring_WebClient", ArserConfig.builder()
                .workingDir(workingDir)
                .build());
    }

    @Bean
    ClientHttpConnector clientHttpConnector() {
        // Doesn't work for download/get Methods!
        //
        // // Create reactor netty HTTP client.
        // final HttpClient httpClient = HttpClient.create()
        //         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100_000)
        //         .doOnConnected(con -> con
        //                 // .addHandlerLast(new ReadTimeoutHandler(100L, TimeUnit.SECONDS))
        //                 .addHandlerFirst(new ReadTimeoutHandler(100L, TimeUnit.SECONDS))
        //                 .addHandlerLast(new WriteTimeoutHandler(100L, TimeUnit.SECONDS))
        //         );
        //
        // // Create a client http connector using above http client.
        // return new ReactorClientHttpConnector(httpClient);

        return new JdkClientHttpConnector();
    }

    @Bean
    ClientHttpRequestFactory clientHttpRequestFactory() {
        return new JdkClientHttpRequestFactory();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository localDeploySnapshots(final ArserInstance arserInstance) {
        final Repository repository = new FileRepository("deploy-snapshots",
                arserInstance.getConfig().getWorkingDir().resolve("deploy-snapshots").toUri(), true);
        arserInstance.addRepository(repository);

        return repository;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradlePlugins(@Value("${arser.workingDir}") final Path workingDir, final WebClient webclient) {
        final String contextRoot = "gradle-plugins";

        return new RemoteRepositoryWebClient(contextRoot,
                URI.create("https://plugins.gradle.org"),
                workingDir.resolve(contextRoot),
                webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradleReleases(@Value("${arser.workingDir}") final Path workingDir, final WebClient webclient) {
        final String contextRoot = "gradle-releases";

        return new RemoteRepositoryWebClient(contextRoot,
                URI.create("https://repo.gradle.org/gradle/libs-releases"),
                workingDir.resolve(contextRoot),
                webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteMavenCentral(@Value("${arser.workingDir}") final Path workingDir, final WebClient webclient) {
        final String contextRoot = "maven-central";

        return new RemoteRepositoryWebClient(contextRoot,
                URI.create("https://repo1.maven.org/maven2"),
                workingDir.resolve(contextRoot),
                webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublic(final ArserInstance arserInstance, final Repository remoteMavenCentral, final Repository remoteGradleReleases, final Repository remoteGradlePlugins) {
        final Repository repository = new VirtualRepository("public", List.of(remoteMavenCentral, remoteGradleReleases, remoteGradlePlugins));
        arserInstance.addRepository(repository);

        return repository;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublicSnapshots(final ArserInstance arserInstance, final Repository localDeploySnapshots) {
        final Repository repository = new VirtualRepository("public-snapshots", List.of(localDeploySnapshots));
        arserInstance.addRepository(repository);

        return repository;
    }

    @Bean
    WebClient webClient(final ClientHttpConnector clientHttpConnector) {
        return WebClient.builder()
                .clientConnector(clientHttpConnector)
                .defaultHeader(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

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
    // WebClient.Builder webClientBuilder() {
    //     return WebClient.builder()
    //             .defaultHeader(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
    //             // .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
    //             ;
    // }
    //
    // @Bean
    // WebClientCustomizer webClientCustomizer(final ClientHttpConnector clientHttpConnector) {
    //     return webClientBuilder -> webClientBuilder.clientConnector(clientHttpConnector);
    // }
}
