package de.freese.arser.blobstore.jdbc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.stream.Collectors;

import javax.sql.DataSource;

import de.freese.arser.blobstore.api.AbstractBlobStore;
import de.freese.arser.blobstore.api.Blob;
import de.freese.arser.blobstore.api.BlobId;
import de.freese.arser.blobvalue.BlobValue;
import de.freese.arser.blobvalue.DefaultBlobValue;
import de.freese.arser.blobvalue.EmptyBlobValue;

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
        catch (SQLException _) {
            // Ignore
        }

        return URI.create("jdbc");
    }

    private final DataSource dataSource;

    private URI uri;

    public JdbcBlobStore(final DataSource dataSource) {
        super();

        this.dataSource = Objects.requireNonNull(dataSource, "dataSource required");
    }

    @Override
    public Blob create(final BlobId id, final InputStream inputStream) throws Exception {
        final String sql = "insert into BLOB_STORE (URI, BLOB) values (?, ?)";

        try (Connection connection = getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                prepareStatement.setString(1, id.uri().toString());
                // prepareStatement.setBlob(2, inputStream);
                prepareStatement.setBinaryStream(2, inputStream);
                prepareStatement.executeUpdate();

                connection.commit();
            }
            catch (final Exception ex) {
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
                prepareStatement.setString(1, id.uri().toString());
                prepareStatement.executeUpdate();

                connection.commit();
            }
            catch (final Exception ex) {
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
            prepareStatement.setString(1, id.uri().toString());

            try (ResultSet resultSet = prepareStatement.executeQuery()) {
                resultSet.next();

                final int result = resultSet.getInt(1);

                return result > 0;
            }
        }
    }

    @Override
    public Blob get(final BlobId id) throws Exception {
        final String sql = "select BLOB from BLOB_STORE where URI = ?";

        try (Connection connection = getDataSource().getConnection()) {
            final PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, id.uri().toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    try (InputStream inputStream = resultSet.getBinaryStream("BLOB")) {
                        final BlobValue blobValue = DefaultBlobValue.of(inputStream);

                        return new JdbcBlob(id, blobValue);
                    }
                }
            }
        }

        return new JdbcBlob(id, new EmptyBlobValue());
    }

    @Override
    public URI getUri() {
        if (uri == null) {
            uri = getUri(dataSource);
        }

        return uri;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }
}
