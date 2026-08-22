// Created: 23 Dez. 2024
package de.freese.arser.spring;

import java.nio.file.Path;

import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * @author Thomas Freese
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SpringServerApplication.class)
@ActiveProfiles("web-client")
class TestSpringWithWebClient extends AbstractTestSpringServer {
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static Path pathTest;

    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("arser.workingDir", () -> pathTest.toAbsolutePath().toString());
    }

    @Override
    protected Path getWorkingDir() {
        return pathTest;
    }
}
