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

import de.freese.arser.config.ArserConfig;
import de.freese.arser.core.repository.FileRepository;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.VirtualRepository;
import de.freese.arser.instance.ArserInstance;
import de.freese.arser.spring.SpringArserInstance;
import de.freese.arser.spring.repository.remote.RemoteRepositoryRequestFactory;

/**
 * @author Thomas Freese
 */
@Configuration
@Profile("request-factory")
public class ArserConfigRequestFactory {
    @Bean(destroyMethod = "shutdown")
    ArserInstance arserInstance(@Value("${arser.workingDir}") final Path workingDir) {
        return new SpringArserInstance("arserInstance_spring_RequestFactory", ArserConfig.builder()
                .workingDir(workingDir)
                .build());
    }

    // @Bean
    // @DependsOn({"virtualPublic", "virtualPublicSnapshots"})
    // ArserInstance arser(final ApplicationContext applicationContext) {
    //     LOGGER.info("configure arser");
    //
    //     final List<Repository> repositories = List.copyOf(applicationContext.getBeansOfType(Repository.class).values());
    //
    //     final ArserInstance arserInstance = new SpringArserInstance();
    //
    //     repositories.forEach(arser::addRepository);
    //
    //     LOGGER.info("arser repository count: {}", ArserInstance.getRepositoryCount());
    //
    //     if (ArserInstance.getRepositoryCount() == 0) {
    //         throw new IllegalStateException("ArserInstance doesn't have any registered repositories");
    //     }
    //
    //     return ArserInstance;
    // }

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
    Repository remoteGradlePlugins(@Value("${arser.workingDir}") final Path workingDir, final ClientHttpRequestFactory clientHttpRequestFactory) {
        final String contextRoot = "gradle-plugins";

        return new RemoteRepositoryRequestFactory(contextRoot,
                URI.create("https://plugins.gradle.org"),
                workingDir.resolve(contextRoot),
                clientHttpRequestFactory);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradleReleases(@Value("${arser.workingDir}") final Path workingDir, final ClientHttpRequestFactory clientHttpRequestFactory) {
        final String contextRoot = "gradle-releases";

        return new RemoteRepositoryRequestFactory(contextRoot,
                URI.create("https://repo.gradle.org/gradle/libs-releases"),
                workingDir.resolve(contextRoot),
                clientHttpRequestFactory);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteMavenCentral(@Value("${arser.workingDir}") final Path workingDir, final ClientHttpRequestFactory clientHttpRequestFactory) {
        final String contextRoot = "maven-central";

        return new RemoteRepositoryRequestFactory(contextRoot,
                URI.create("https://repo1.maven.org/maven2"),
                workingDir.resolve(contextRoot),
                clientHttpRequestFactory);
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
}
