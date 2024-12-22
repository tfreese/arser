// Created: 21.01.24
package de.freese.arser.spring.config;

import java.net.URI;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import de.freese.arser.Arser;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;
import de.freese.arser.spring.repository.remote.SpringRemoteRepositoryClientHttpRequestFactory;

/**
 * @author Thomas Freese
 */
@Configuration
@Profile("web")
public class ArserConfigWeb {
    // private static final Logger LOGGER = LoggerFactory.getLogger(ArserConfigWeb.class);

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

    //    @Bean(initMethod = "start", destroyMethod = "stop")
    //    JreHttpClientComponent jreHttpClientComponent() {
    //        final HttpClientConfig httpClientConfig = new HttpClientConfig();
    //        httpClientConfig.setThreadNamePattern("http-client-%d");
    //        httpClientConfig.setThreadPoolCoreSize(2);
    //        httpClientConfig.setThreadPoolMaxSize(6);
    //
    //        return new JreHttpClientComponent(httpClientConfig);
    //    }

    @Bean
    ClientHttpRequestFactory clientHttpRequestFactory() {
        return new JdkClientHttpRequestFactory();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository localDeploySnapshots() {
        return new FileRepository("deploy-snapshots", URI.create("file:///tmp/arser/deploy-snapshots"), true);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradlePlugins(final ClientHttpRequestFactory clientHttpRequestFactory) {
        return new SpringRemoteRepositoryClientHttpRequestFactory("gradle-plugins", URI.create("https://plugins.gradle.org"), clientHttpRequestFactory);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradleReleases(final ClientHttpRequestFactory clientHttpRequestFactory) {
        return new SpringRemoteRepositoryClientHttpRequestFactory("gradle-releases", URI.create("https://repo.gradle.org/gradle/libs-releases"), clientHttpRequestFactory);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteMavenCentral(final ClientHttpRequestFactory clientHttpRequestFactory) {
        return new SpringRemoteRepositoryClientHttpRequestFactory("maven-central", URI.create("https://repo1.maven.org/maven2"), clientHttpRequestFactory);
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
}
