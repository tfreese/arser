// Created: 21.01.24
package de.freese.arser.spring.config;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import de.freese.arser.blobstore.file.FileBlobStore;
import de.freese.arser.core.Arser;
import de.freese.arser.core.component.BlobStoreComponent;
import de.freese.arser.core.config.LocalRepositoryConfig;
import de.freese.arser.core.config.VirtualRepositoryConfig;
import de.freese.arser.core.repository.CachedRepository;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.spring.repository.remote.SpringRemoteRepositoryWebClient;

/**
 * @author Thomas Freese
 */
@Configuration
@Profile("web-client")
public class ArserConfigWebClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArserConfigWebClient.class);

    @Value("${arser.workingDir}")
    private URI workingDir;

    @Bean
    @DependsOn({"virtualPublic", "virtualPublicSnapshots"})
    Arser arser(final ApplicationContext applicationContext) {
        LOGGER.info("configure arser");

        final Arser arser = new Arser();

        for (String beanName : applicationContext.getBeanNamesForType(Repository.class)) {
            final Repository repository = applicationContext.getBean(beanName, Repository.class);
            arser.addRepository(repository);
        }

        LOGGER.info("arser repository count: {}", arser.getRepositoryCount());

        if (arser.getRepositoryCount() == 0) {
            throw new IllegalStateException("arser doesn't have any registered repositories");
        }

        return arser;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    BlobStoreComponent blobStoreComponentGradleReleases() {
        return new BlobStoreComponent(new FileBlobStore(URI.create("file:///tmp/arser/cache/gradle/libs-releases")));
    }

    @Bean
    ClientHttpConnector clientHttpConnector() {
        return new JdkClientHttpConnector();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository localDeploySnapshots() {
        return new FileRepository(LocalRepositoryConfig.builder()
                .name("deploy-snapshots")
                .uri(URI.create("file:///tmp/arser/deploy-snapshots"))
                .writeable(true)
                .build()
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradlePlugins(final WebClient webclient) {
        final Path tempDir = Path.of(workingDir).resolve("temp");

        return new SpringRemoteRepositoryWebClient("gradle-plugins", URI.create("https://plugins.gradle.org"), webclient, tempDir);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradleReleases(final WebClient webclient, final BlobStoreComponent blobStoreComponentGradleReleases) {
        final Path tempDir = Path.of(workingDir).resolve("temp");

        final Repository repository = new SpringRemoteRepositoryWebClient("gradle-releases", URI.create("https://repo.gradle.org/gradle/libs-releases"), webclient, tempDir);

        return new CachedRepository(repository, blobStoreComponentGradleReleases.getBlobStore());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteMavenCentral(final WebClient webclient) {
        final Path tempDir = Path.of(workingDir).resolve("temp");

        return new SpringRemoteRepositoryWebClient("maven-central", URI.create("https://repo1.maven.org/maven2"), webclient, tempDir);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublic(final Repository remoteMavenCentral, final Repository remoteGradleReleases, final Repository remoteGradlePlugins) {
        return new VirtualRepository(VirtualRepositoryConfig.builder()
                .name("public")
                .repositories(List.of(remoteMavenCentral, remoteGradleReleases, remoteGradlePlugins))
                .build()
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublicSnapshots(final Repository localDeploySnapshots) {
        return new VirtualRepository(VirtualRepositoryConfig.builder()
                .name("public-snapshots")
                .repositories(List.of(localDeploySnapshots))
                .build()
        );
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
