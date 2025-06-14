package de.freese.arser.blobstore.jdbc;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import de.freese.arser.blobstore.api.AbstractBlobStore;
import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobstore.api.ThrowingConsumer;

/**
 * @author Thomas Freese
 */
@SuppressWarnings({"java:S2095", "java:S1141", "java:S4174"})
public class JdbcBlobStore extends AbstractBlobStore {

    private static URI getUri(final DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            final String url = connection.getMetaData().getURL();

            return URI.create(url);
        }
        catch (SQLException ex) {
            // Ignore
        }

        return URI.create("jdbc");
    }

    private final Supplier<DataSource> dataSourceSupplier;

    private URI uri;

    public JdbcBlobStore(final Supplier<DataSource> dataSourceSupplier) {
        super();

        this.dataSourceSupplier = Objects.requireNonNull(dataSourceSupplier, "Supplier<DataSource> required");
    }

    @Override
    public Blob create(final BlobId id, final ThrowingConsumer<OutputStream, Exception> consumer) throws Exception {
        final String sql = "insert into BLOB_STORE (URI, BLOB) values (?, ?)";

        Exception exception = null;

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            final java.sql.Blob blob = connection.createBlob();

            try (OutputStream outputStream = new BufferedOutputStream(blob.setBinaryStream(1))) {
                consumer.accept(outputStream);

                outputStream.flush();
            }

            try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                prepareStatement.setString(1, id.getUri().toString());
                prepareStatement.setBlob(2, blob);
                prepareStatement.executeUpdate();

                connection.commit();
            }
            catch (SQLException ex) {
                exception = ex;

                try {
                    connection.rollback();
                }
                catch (SQLException ex1) {
                    exception = ex1;
                }
            }

            try {
                blob.free();
            }
            catch (SQLException ex) {
                exception = ex;
            }
        }
        catch (Exception ex) {
            exception = ex;
        }

        if (exception != null) {
            getLogger().error(exception.getMessage(), exception);
            throw exception;
        }

        return get(id);
    }

    @Override
    public Blob create(final BlobId id, final InputStream inputStream) throws Exception {
        final String sql = "insert into BLOB_STORE (URI, BLOB) values (?, ?)";

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                prepareStatement.setString(1, id.getUri().toString());
                // prepareStatement.setBlob(2, inputStream);
                prepareStatement.setBinaryStream(2, inputStream);
                prepareStatement.executeUpdate();

                connection.commit();
            }
            catch (Exception ex) {
                connection.rollback();

                throw ex;
            }
        }

        return get(id);
    }

    public void createDatabaseIfNotExist() throws Exception {
        boolean databaseExists = false;

        try (Connection connection = getDataSource().getConnection()) {
            final ResultSet resultSet = connection.getMetaData().getTables(null, null, "BLOB_STORE", null);

            if (resultSet.next()) {
                databaseExists = true;
            }
        }

        if (databaseExists) {
            return;
        }

        getLogger().info("Lookup for blobstore.sql");
        final URL url = Thread.currentThread().getContextClassLoader().getResource("jdbc/blobstore.sql");

        if (url == null) {
            throw new IllegalArgumentException("no sql script found");
        }

        getLogger().info("SQL found: {}", url);

        try (Connection connection = getDataSource().getConnection();
             Statement statement = connection.createStatement();
             InputStream inputStream = url.openStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            final String script = bufferedReader.lines()
                    .filter(Objects::nonNull)
                    .filter(l -> !l.isEmpty())
                    .filter(l -> !l.startsWith("--"))
                    .filter(l -> !l.startsWith("#"))
                    //.map(l -> l.replace("( ", " ").replace(" )", ")").replace("  "," "))
                    .map(String::strip)
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.joining(" "));

            // SQLs ending with ';'.
            try (Scanner scanner = new Scanner(script)) {
                scanner.useDelimiter(";");

                while (scanner.hasNext()) {
                    final String sql = scanner.next().strip();
                    getLogger().info("execute: {}", sql);
                    // statement.execute(sql);
                    statement.addBatch(sql);
                }

                statement.executeBatch();
            }
        }
    }

    @Override
    public void delete(final BlobId id) throws Exception {
        final String sql = "delete from BLOB_STORE where URI = ?";

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                prepareStatement.setString(1, id.getUri().toString());
                prepareStatement.executeUpdate();

                connection.commit();
            }
            catch (Exception ex) {
                connection.rollback();

                throw ex;
            }
        }
    }

    @Override
    public boolean exists(final BlobId id) throws Exception {
        final String sql = "select count(*) from BLOB_STORE where URI = ?";

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
            prepareStatement.setString(1, id.getUri().toString());

            try (ResultSet resultSet = prepareStatement.executeQuery()) {
                resultSet.next();

                final int result = resultSet.getInt(1);

                return result > 0;
            }
        }
    }

    @Override
    public Blob get(final BlobId id) throws Exception {
        return new JdbcBlob(id, this);
    }

    @Override
    public URI getUri() {
        if (uri == null) {
            final DataSource dataSource = getDataSource();

            if (dataSource == null) {
                return URI.create("jdbc");
            }

            uri = getUri(dataSource);
        }

        return uri;
    }

    void consume(final BlobId id, final ThrowingConsumer<InputStream, Exception> consumer) throws Exception {
        final String sql = "select BLOB from BLOB_STORE where URI = ?";

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id.getUri().toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    try (InputStream inputStream = InputStream.nullInputStream()) {
                        consumer.accept(inputStream);
                    }
                }
                else {
                    try (InputStream inputStream = resultSet.getBinaryStream("BLOB")) {
                        consumer.accept(inputStream);
                    }
                }
            }
        }
    }

    InputStream inputStream(final BlobId id) throws Exception {
        final String sql = "select BLOB from BLOB_STORE where URI = ?";

        final Connection connection = getDataSource().getConnection();

        final PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, id.getUri().toString());

        final ResultSet resultSet = preparedStatement.executeQuery();

        if (!resultSet.next()) {
            return InputStream.nullInputStream();
        }

        // final java.sql.Blob blob = resultSet.getBlob("BLOB");

        // return new FilterInputStream(blob.getBinaryStream()) {
        return new FilterInputStream(resultSet.getBinaryStream("BLOB")) {
            @Override
            public void close() throws IOException {
                super.close();

                SQLException exception = null;

                // try {
                //     blob.free();
                // }
                // catch (SQLException ex) {
                //     getLogger().error("Blob.free: " + ex.getMessage(), ex);
                //     exception = ex;
                // }

                try {
                    resultSet.close();
                }
                catch (SQLException ex) {
                    getLogger().error("ResultSet.close: %s".formatted(ex.getMessage()), ex);
                    exception = ex;
                }

                try {
                    preparedStatement.close();
                }
                catch (SQLException ex) {
                    getLogger().error("PreparedStatement.close: %s".formatted(ex.getMessage()), ex);
                    exception = ex;
                }

                try {
                    connection.close();
                }
                catch (SQLException ex) {
                    getLogger().error("Connection.close: %s".formatted(ex.getMessage()), ex);
                    exception = ex;
                }

                if (exception != null) {
                    throw new IOException(exception);
                }
            }
        };
    }

    long length(final BlobId id) throws Exception {
        final String sql = "select BLOB from BLOB_STORE where URI = ?";

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
            prepareStatement.setString(1, id.getUri().toString());

            try (ResultSet resultSet = prepareStatement.executeQuery()) {
                if (resultSet.next()) {
                    final java.sql.Blob blob = resultSet.getBlob("BLOB");

                    return blob.length();
                }
            }
        }

        return -1;
    }

    protected DataSource getDataSource() {
        return dataSourceSupplier.get();
    }
}
