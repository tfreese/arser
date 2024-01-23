// Created: 24.07.23
package de.freese.arser.core.component;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.freese.arser.config.StoreConfig;
import de.freese.arser.core.lifecycle.AbstractLifecycle;

/**
 * @author Thomas Freese
 */
public class DatasourceComponent extends AbstractLifecycle {

    private final String poolName;
    private final StoreConfig storeConfig;

    private DataSource dataSource;

    public DatasourceComponent(final StoreConfig storeConfig, final String poolName) {
        super();

        this.storeConfig = assertNotNull(storeConfig, () -> "StoreConfig");
        this.poolName = assertNotNull(poolName, () -> "PoolName");
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
        super.doStart();

        assertNotNull(storeConfig, () -> "StoreConfig");

        final HikariConfig config = new HikariConfig();
        config.setDriverClassName(assertNotNull(storeConfig.getDriverClassName(), () -> "DriverClassName"));
        config.setJdbcUrl(assertNotNull(storeConfig.getUri(), () -> "Uri"));
        config.setUsername(assertNotNull(storeConfig.getUser(), () -> "User"));
        config.setPassword(assertNotNull(storeConfig.getPassword(), () -> "Password"));
        config.setMinimumIdle(assertValue(storeConfig.getPoolCoreSize(), value -> value <= 0, () -> "PoolCoreSize has invalid range"));
        config.setMaximumPoolSize(assertValue(storeConfig.getPoolMaxSize(), value -> value <= 0, () -> "PoolMaxSize has invalid range"));
        config.setPoolName(assertNotNull(poolName, () -> "PoolName"));
        config.setAutoCommit(false);

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

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
