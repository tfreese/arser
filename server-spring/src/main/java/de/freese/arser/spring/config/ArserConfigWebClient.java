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

import de.freese.arser.Arser;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.spring.repository.remote.RemoteRepositoryWebClient;

/**
 * @author Thomas Freese
 */
@Configuration
@Profile("web-client")
public class ArserConfigWebClient {
    // private static final Logger LOGGER = LoggerFactory.getLogger(ArserConfigWebClient.class);

    // @Value("${arser.workingDir}")
    // private Path workingDir;

    @Bean
    Arser arser() {
        return new Arser();
    }

    // @Bean
    // @DependsOn({"virtualPublic", "virtualPublicSnapshots"})
    // Arser arser(final ApplicationContext applicationContext) {
    //     LOGGER.info("configure arser");
    //
    //     final List<Repository> repositories = List.copyOf(applicationContext.getBeansOfType(Repository.class).values());
    //
    //     final Arser arser = new Arser();
    //
    //     repositories.forEach(arser::addRepository);
    //
    //     LOGGER.info("arser repository count: {}", arser.getRepositoryCount());
    //
    //     if (arser.getRepositoryCount() == 0) {
    //         throw new IllegalStateException("arser doesn't have any registered repositories");
    //     }
    //
    //     return arser;
    // }

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
    Repository localDeploySnapshots(final Arser arser, @Value("${arser.workingDir}") final Path workingDir) {
        final Repository repository = new FileRepository("deploy-snapshots", workingDir.resolve("deploy-snapshots").toUri(), true);
        arser.addRepository(repository);

        return repository;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradlePlugins(final WebClient webclient) {
        return new RemoteRepositoryWebClient("gradle-plugins", URI.create("https://plugins.gradle.org"), webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradleReleases(final WebClient webclient) {
        return new RemoteRepositoryWebClient("gradle-releases", URI.create("https://repo.gradle.org/gradle/libs-releases"), webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteMavenCentral(final WebClient webclient) {
        return new RemoteRepositoryWebClient("maven-central", URI.create("https://repo1.maven.org/maven2"), webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublic(final Arser arser, final Repository remoteMavenCentral, final Repository remoteGradleReleases, final Repository remoteGradlePlugins) {
        final Repository repository = new VirtualRepository("public", List.of(remoteMavenCentral, remoteGradleReleases, remoteGradlePlugins));
        arser.addRepository(repository);

        return repository;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublicSnapshots(final Arser arser, final Repository localDeploySnapshots) {
        final Repository repository = new VirtualRepository("public-snapshots", List.of(localDeploySnapshots));
        arser.addRepository(repository);

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
