// Created: 21.01.24
package de.freese.arser.spring.config;

import java.net.URI;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import de.freese.arser.core.Arser;
import de.freese.arser.core.repository.Repository;
import de.freese.arser.core.repository.local.FileRepository;
import de.freese.arser.core.repository.virtual.VirtualRepository;
import de.freese.arser.core.utils.ArserUtils;
import de.freese.arser.spring.repository.remote.SpringWebClientRemoteRepository;

/**
 * @author Thomas Freese
 */
@Configuration
public class ArserConfig {
    @Bean
    Arser arser(final ApplicationContext applicationContext) {
        final Arser arser = new Arser();

        for (String beanName : applicationContext.getBeanNamesForType(Repository.class)) {
            final Repository repository = applicationContext.getBean(beanName, Repository.class);
            arser.addRepository(repository);
        }

        return arser;
    }

    //    @Bean(initMethod = "start", destroyMethod = "stop")
    //    JreHttpClientComponent jreHttpClientComponent() {
    //        final HttpClientConfig httpClientConfig = new HttpClientConfig();
    //        httpClientConfig.setThreadNamePattern("http-client-%d");
    //        httpClientConfig.setThreadPoolCoreSize(2);
    //        httpClientConfig.setThreadPoolMaxSize(6);
    //
    //        return new JreHttpClientComponent(httpClientConfig);
    //    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository localDeploySnapshots() {
        return new FileRepository("deploy-snapshots", URI.create("file:///tmp/arser/deploy-snapshots"), true);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteGradleReleases(final WebClient webclient) {
        return new SpringWebClientRemoteRepository("gradle-releases", URI.create("https://repo.gradle.org/gradle/libs-releases"), webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remoteMavenCentral(final WebClient webclient) {
        return new SpringWebClientRemoteRepository("maven-central", URI.create("https://repo1.maven.org/maven2"), webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository remotePrimefaces(final WebClient webclient) {
        return new SpringWebClientRemoteRepository("primefaces", URI.create("https://repository.primefaces.org"), webclient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublic(final ApplicationContext applicationContext) {
        final VirtualRepository virtualRepository = new VirtualRepository("public");

        virtualRepository.add(applicationContext.getBean("remoteMavenCentral", Repository.class));
        virtualRepository.add(applicationContext.getBean("remotePrimefaces", Repository.class));
        virtualRepository.add(applicationContext.getBean("remoteGradleReleases", Repository.class));

        return virtualRepository;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublicLocal(final ApplicationContext applicationContext) {
        final VirtualRepository virtualRepository = new VirtualRepository("public-local");

        virtualRepository.add(applicationContext.getBean("localDeploySnapshots", Repository.class));

        return virtualRepository;
    }

    @Bean
    WebClient webClient(final WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    WebClient.Builder webClientBuilder() {
        // @formatter:off
        return WebClient.builder()
                .defaultHeader(ArserUtils.HTTP_HEADER_USER_AGENT, ArserUtils.SERVER_NAME)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024));
        // @formatter:on
    }
}
