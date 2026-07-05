// Created: 23 Dez. 2024
package de.freese.arser.spring;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Thomas Freese
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"arser.workingDir=/tmp/arser-spring-web-client"}
)
@ActiveProfiles("web-client")
class TestSpringWithWebClient extends AbstractTestSpringServer {
    private static final Path PATH_TEST = Path.of(System.getProperty("java.io.tmpdir"), "arser-spring-web-client");

    @AfterAll
    static void afterAll() throws IOException {
        afterAll(PATH_TEST);
    }

    @Override
    protected Path getWorkingDir() {
        return PATH_TEST;
    }
}
