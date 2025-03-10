package de.freese.arser.blobstore.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.bridge.SLF4JBridgeHandler;

import de.freese.arser.blobstore.empty.EmptyBlobStore;
import de.freese.arser.blobstore.file.FileBlobStore;
import de.freese.arser.blobstore.jdbc.JdbcBlobStore;
import de.freese.arser.blobstore.memory.MemoryBlobStore;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestBlobStore {
    private static final Path PATH_TEST = Paths.get(System.getProperty("java.io.tmpdir"), "arser-test-blobStore");

    private static DataSource dataSourceDerby;
    private static DataSource dataSourceH2;
    private static DataSource dataSourceHsqldb;

    @AfterAll
    static void afterAll() throws Exception {
        Files.walkFileTree(PATH_TEST, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });

        for (final DataSource dataSource : List.of(dataSourceH2, dataSourceHsqldb, dataSourceDerby)) {
            if (dataSource instanceof AutoCloseable ac) {
                ac.close();
            }
            //            else if (dataSource instanceof DisposableBean db) {
            //                db.destroy();
            //            }
        }
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        // Disable JUL-Logger.
        // LogManager.getLogManager().reset();

        // Redirect JUL-Logger to slf4j.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        Files.createDirectories(PATH_TEST);

        final BiConsumer<String, HikariConfig> hikariConfigurer = (poolName, config) -> {
            config.setUsername("sa");
            config.setPassword("");
            config.setMinimumIdle(1);
            config.setMaximumPoolSize(3);
            config.setPoolName(poolName);
            config.setAutoCommit(true);
        };

        // org/springframework/boot/jdbc/DatabaseDriver.java

        // H2
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        hikariConfigurer.accept("h2", config);
        dataSourceH2 = new HikariDataSource(config);

        // Hsqldb
        config = new HikariConfig();
        config.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        config.setJdbcUrl("jdbc:hsqldb:mem:test;shutdown=true");
        hikariConfigurer.accept("hsqldb", config);
        dataSourceHsqldb = new HikariDataSource(config);

        // Derby
        config = new HikariConfig();
        config.setDriverClassName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        config.setJdbcUrl("jdbc:derby:memory:test;create=true");
        hikariConfigurer.accept("derby", config);
        dataSourceDerby = new HikariDataSource(config);
    }

    static Stream<Arguments> createArgumentes() {
        return Stream.of(
                Arguments.of("Memory", new MemoryBlobStore()),
                Arguments.of("File", new FileBlobStore(PATH_TEST.toUri())),
                Arguments.of("DataSource-H2", new JdbcBlobStore(() -> dataSourceH2)),
                Arguments.of("DataSource-HSQLDB", new JdbcBlobStore(() -> dataSourceHsqldb)),
                Arguments.of("DataSource-Derby", new JdbcBlobStore(() -> dataSourceDerby))
        );
    }

    @AfterEach
    void afterEach() {
        // Empty
    }

    @BeforeEach
    void beforeEach() {
        // Empty
    }

    @Test
    void testEmpty() throws Exception {
        final BlobStore blobStore = new EmptyBlobStore();
        assertEquals("empty", blobStore.getUri().toString());

        final URI uri = URI.create("not_existing");
        final BlobId blobId = new BlobId(uri);
        assertFalse(blobStore.exists(blobId));

        final Blob blob = blobStore.get(blobId);
        assertNotNull(blob);
        assertEquals("empty", blob.getId().getUri().toString());
        assertEquals(-1, blob.getLength());

        try (InputStream inputStream = blob.createInputStream()) {
            inputStream.transferTo(OutputStream.nullOutputStream());
        }

        try (InputStream inputStream = InputStream.nullInputStream()) {
            blobStore.create(blobId, inputStream);
        }

        try (InputStream inputStream = InputStream.nullInputStream()) {
            blobStore.create(blobId, inputStream::transferTo);
        }
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArgumentes")
    void testInputStream(final String name, final BlobStore blobStore) throws Exception {
        if (blobStore instanceof JdbcBlobStore dsBs) {
            dsBs.createDatabaseIfNotExist();
        }

        final Path path = Paths.get("build.gradle");
        final long fileSize = Files.size(path);
        final byte[] bytes = Files.readAllBytes(path);

        final URI uri = path.toUri();
        final BlobId blobId = new BlobId(uri);

        assertFalse(blobStore.exists(blobId));

        // Insert
        try (InputStream inputStream = Files.newInputStream(path)) {
            blobStore.create(blobId, inputStream);
        }

        testAfterInsert(blobStore, blobId, uri, fileSize, bytes);
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArgumentes")
    void testNotExistingUri(final String name, final BlobStore blobStore) throws Exception {
        if (blobStore instanceof JdbcBlobStore dsBs) {
            dsBs.createDatabaseIfNotExist();
        }

        final URI uri = URI.create("not_existing");
        final BlobId blobId = new BlobId(uri);
        assertFalse(blobStore.exists(blobId));

        // Select
        final Blob blob = blobStore.get(blobId);
        assertNotNull(blob);
        assertEquals(-1, blob.getLength());
        assertEquals(uri, blob.getId().getUri());
        assertArrayEquals(new byte[0], blob.getAllBytes());

        // Delete
        blobStore.delete(blobId);
        assertFalse(blobStore.exists(blobId));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArgumentes")
    void testOutputStream(final String name, final BlobStore blobStore) throws Exception {
        if (blobStore instanceof JdbcBlobStore jdbcBlobStore) {
            jdbcBlobStore.createDatabaseIfNotExist();
        }

        final Path path = Paths.get("build.gradle");
        final long fileSize = Files.size(path);
        final byte[] bytes = Files.readAllBytes(path);

        final URI uri = path.toUri();
        final BlobId blobId = new BlobId(uri);

        assertFalse(blobStore.exists(blobId));

        // Insert
        try (InputStream inputStream = Files.newInputStream(path)) {
            blobStore.create(blobId, inputStream::transferTo);
        }

        testAfterInsert(blobStore, blobId, uri, fileSize, bytes);
    }

    protected void testAfterInsert(final BlobStore blobStore, final BlobId blobId, final URI uri, final long fileSize, final byte[] bytes) throws Exception {
        assertTrue(blobStore.exists(blobId));

        // Select
        final Blob blob = blobStore.get(blobId);
        assertNotNull(blob);
        assertEquals(fileSize, blob.getLength());
        assertEquals(uri, blob.getId().getUri());
        assertArrayEquals(bytes, blob.getAllBytes());

        blob.consume(inputStream -> {
            // Doesn't work with H2.
            // assertEquals(fileSize, inputStream.available());

            assertArrayEquals(bytes, blob.getAllBytes());
        });

        // Delete
        blobStore.delete(blobId);
        assertFalse(blobStore.exists(blobId));
    }
}
