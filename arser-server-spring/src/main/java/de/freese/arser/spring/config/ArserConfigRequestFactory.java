package de.freese.arser.spring.config;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import de.freese.arser.api.Arser;
import de.freese.arser.api.ArserConfig;
import de.freese.arser.api.ArserRequest;
import de.freese.arser.api.ArserResult;
import de.freese.arser.component.EmptyLifeCycleRegistry;
import de.freese.arser.repository.Repository;
import de.freese.arser.repository.file.FileRepository;
import de.freese.arser.repository.file.FileRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryRequestFactory;
import de.freese.arser.repository.virtual.VirtualRepository;
import de.freese.arser.repository.virtual.VirtualRepositoryConfig;

/**
 * @author Thomas Freese
 * @since 21.01.24
 */
@Configuration
@Profile("request-factory")
public class ArserConfigRequestFactory {

    @Bean
    @DependsOn({"virtualPublic"})
    Arser arser(final ApplicationContext applicationContext) {
        final Collection<Repository> repositories = applicationContext.getBeansOfType(Repository.class).values();

        final Map<String, Repository> repositoryMap = repositories.stream().collect(Collectors.toConcurrentMap(Repository::getName, Function.identity()));

        return new Arser() {
            @Override
            public ArserResult download(final String repositoryName, final ArserRequest arserRequest) {
                return getRepository(repositoryName).download(arserRequest);
            }

            @Override
            public ArserResult exist(final String repositoryName, final ArserRequest arserRequest) {
                return getRepository(repositoryName).exist(arserRequest);
            }

            @Override
            public ArserConfig getConfig() {
                return null;
            }

            @Override
            public Map<String, Repository> getRepositories() {
                return Map.copyOf(repositoryMap);
            }

            @Override
            public ArserResult upload(final String repositoryName, final ArserRequest arserRequest, final InputStream inputStream) {
                return getRepository(repositoryName).upload(arserRequest, inputStream);
            }

            private Repository getRepository(final String repositoryName) {
                final Repository repository = repositoryMap.get(repositoryName);

                if (repository == null) {
                    throw new IllegalStateException("Repository not found: " + repositoryName);
                }

                return repository;
            }
        };
    }

    @Bean
    ClientHttpRequestFactory clientHttpRequestFactory() {
        return new JdkClientHttpRequestFactory();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository httpGradleLibsReleases(@Value("${arser.workingDir}") final Path workingDir, final ClientHttpRequestFactory clientHttpRequestFactory) {
        final HttpRepositoryConfig config = HttpRepositoryConfig.builder()
                .name("gradle-libs-releases")
                .uri(URI.create("https://repo.gradle.org/gradle/libs-releases"))
                .logging(true)
                .cachingPath(workingDir.resolve("cache").resolve("gradle-libs-releases"))
                .connectTimeout(Duration.ofSeconds(10L))
                .maxRetries(3)
                .retryInterval(Duration.ofSeconds(2L))
                .build();

        return new HttpRepositoryRequestFactory(config, clientHttpRequestFactory);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository httpMavenCentral(@Value("${arser.workingDir}") final Path workingDir, final ClientHttpRequestFactory clientHttpRequestFactory) {
        final HttpRepositoryConfig config = HttpRepositoryConfig.builder()
                .name("maven-central")
                .uri(URI.create("https://repo1.maven.org/maven2"))
                .logging(true)
                .cachingPath(workingDir.resolve("cache").resolve("maven-central"))
                .connectTimeout(Duration.ofSeconds(10L))
                .maxRetries(3)
                .retryInterval(Duration.ofSeconds(2L))
                .build();

        return new HttpRepositoryRequestFactory(config, clientHttpRequestFactory);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository localSnapshots(@Value("${arser.workingDir}") final Path workingDir) {
        final FileRepositoryConfig config = FileRepositoryConfig.builder()
                .name("snapshots")
                .uri(workingDir.resolve("local").resolve("snapshots").toUri())
                .logging(true)
                .readOnly(false)
                .build();

        return FileRepository.of(config, EmptyLifeCycleRegistry.INSTANCE);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository virtualPublic(
            final @Qualifier("httpMavenCentral") Repository httpMavenCentral,
            final @Qualifier("httpGradleLibsReleases") Repository httpGradleLibsReleases,
            final @Qualifier("localSnapshots") Repository localSnapshots) {

        final Map<String, Repository> repositoryMap = new LinkedHashMap<>();
        repositoryMap.put(httpMavenCentral.getName(), httpMavenCentral);
        repositoryMap.put(httpGradleLibsReleases.getName(), httpGradleLibsReleases);
        repositoryMap.put(localSnapshots.getName(), localSnapshots);

        final VirtualRepositoryConfig.VirtualRepositoryConfigBuilder builder = VirtualRepositoryConfig.builder()
                .name("public")
                .uri(URI.create("virtual"))
                .logging(true);
        repositoryMap.keySet().forEach(builder::addRepositoryRef);

        final VirtualRepositoryConfig config = builder.build();

        return VirtualRepository.of(config, repositoryMap::get);
    }
}
