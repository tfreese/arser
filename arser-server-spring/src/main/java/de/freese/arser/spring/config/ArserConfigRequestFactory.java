package de.freese.arser.spring.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import de.freese.arser.repository.Repository;
import de.freese.arser.repository.http.HttpRepositoryConfig;
import de.freese.arser.repository.http.HttpRepositoryRequestFactory;

/**
 * @author Thomas Freese
 * @since 21.01.24
 */
@Configuration
@Profile({"request-factory", "default"})
public class ArserConfigRequestFactory extends AbstractArserConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository httpGradleLibsReleases(@Value("${arser.workingDir}") final Path workingDir) {
        final HttpRepositoryConfig config = createGradleLibsReleasesConfig(workingDir);

        return HttpRepositoryRequestFactory.of(config);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Repository httpMavenCentral(@Value("${arser.workingDir}") final Path workingDir) {
        final HttpRepositoryConfig config = createMavenCentralConfig(workingDir);

        return HttpRepositoryRequestFactory.of(config);
    }
}
