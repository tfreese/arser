// Created: 24.07.23
package de.freese.arser.core.component;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.freese.arser.config.DatabaseStoreConfig;
import de.freese.arser.core.lifecycle.AbstractLifecycle;

/**
 * @author Thomas Freese
 */
public class DatasourceComponent extends AbstractLifecycle {

    private final DatabaseStoreConfig storeConfig;

    private DataSource dataSource;

    public DatasourceComponent(final DatabaseStoreConfig storeConfig) {
        super();

        this.storeConfig = Objects.requireNonNull(storeConfig, "storeConfig required");
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [");
        sb.append("uri=").append(storeConfig.getUri());
        sb.append(']');

        return sb.toString();
    }

    @Override
    protected void doStart() throws Exception {
        final HikariConfig config = new HikariConfig();
        config.setDriverClassName(storeConfig.getDriverClassName());
        config.setJdbcUrl(storeConfig.getUri().toString());
        config.setUsername(storeConfig.getUser());
        config.setPassword(storeConfig.getPassword());
        config.setMinimumIdle(storeConfig.getPoolCoreSize());
        config.setMaximumPoolSize(storeConfig.getPoolMaxSize());
        config.setPoolName(storeConfig.getPoolName());
        config.setAutoCommit(false);

        dataSource = new HikariDataSource(config);
    }

    @Override
    protected void doStop() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            final String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();

            // Handled already by hsql with 'shutdown=true'.
            if (productName.contains("h2") || productName.contains("hsql")) {
                try (Statement statement = connection.createStatement()) {
                    getLogger().info("Execute shutdown command for Database '{}'", productName);
                    statement.execute("SHUTDOWN COMPACT");
                }
            }
        }

        if (dataSource instanceof Closeable c) {
            c.close();
        }
        else if (dataSource instanceof AutoCloseable ac) {
            ac.close();
        }
    }
}
