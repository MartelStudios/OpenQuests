package com.martelstudios.openquests.core.persistence.jdbc;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.persistence.QuestStorageException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A bounded set of connections, checked before being trusted, so the backend depends on a driver
 * and nothing else.
 *
 * <p>Idle connections are kept rather than closed, so a save pass opens nothing in steady state.
 */
public class JdbcConnectionPool implements AutoCloseable {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * How long a connection is trusted without a liveness check, which costs a round trip. A
     * database or a proxy in between drops idle connections.
     */
    private static final long VALIDATE_AFTER_IDLE_MILLIS = 30_000;

    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final Driver driver;
    private final String url;
    private final Properties properties;
    private final int acquireTimeoutSeconds;

    private final Semaphore permits;
    private final ConcurrentLinkedDeque<Pooled> idle = new ConcurrentLinkedDeque<>();

    private volatile boolean closed;

    public JdbcConnectionPool(@Nonnull Driver driver, @Nonnull String url, @Nonnull Properties properties, int size, int acquireTimeoutSeconds) {
        this.driver = driver;
        this.url = url;
        this.properties = properties;
        this.permits = new Semaphore(Math.max(1, size));
        this.acquireTimeoutSeconds = acquireTimeoutSeconds;
    }

    /**
     * Gives the connection back whatever happens, auto-commit restored, so a caller opening a
     * transaction never leaves one behind.
     */
    public <T> T with(@Nonnull SqlWork<T> work) {
        Connection connection = acquire();

        try {
            return work.run(connection);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new QuestStorageException("Quest storage query failed", e);
        } finally {
            release(connection);
        }
    }

    /**
     * Runs a unit of work inside one transaction, committing it if it returns and rolling it back
     * if it does not.
     */
    public <T> T inTransaction(@Nonnull SqlWork<T> work) {
        return with(connection -> {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            }
        });
    }

    @Nonnull
    private Connection acquire() {
        if (closed) throw new QuestStorageException("The quest storage connection pool is closed");

        try {
            if (!permits.tryAcquire(acquireTimeoutSeconds, TimeUnit.SECONDS)) {
                throw new QuestStorageException("No quest storage connection came free within " + acquireTimeoutSeconds + "s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QuestStorageException("Interrupted while waiting for a quest storage connection", e);
        }

        try {
            Pooled pooled;
            while ((pooled = idle.pollLast()) != null) {
                if (pooled.isUsable()) return pooled.connection;
                closeQuietly(pooled.connection);
            }

            return open();
        } catch (RuntimeException e) {
            permits.release();
            throw e;
        }
    }

    private void release(@Nullable Connection connection) {
        if (connection == null) return;

        try {
            if (closed || connection.isClosed()) {
                closeQuietly(connection);
                return;
            }

            if (!connection.getAutoCommit()) connection.setAutoCommit(true);
            idle.addLast(new Pooled(connection, System.currentTimeMillis()));
        } catch (SQLException e) {
            LOGGER.atWarning().withCause(e).log("Dropping a quest storage connection that could not be reset");
            closeQuietly(connection);
        } finally {
            permits.release();
        }
    }

    @Nonnull
    private Connection open() {
        try {
            Connection connection = driver.connect(url, properties);
            if (connection == null) throw new QuestStorageException("The JDBC driver refused " + url);

            return connection;
        } catch (SQLException e) {
            throw new QuestStorageException("Failed to connect to " + url, e);
        }
    }

    @Override
    public void close() {
        closed = true;

        Pooled pooled;
        while ((pooled = idle.pollLast()) != null) {
            closeQuietly(pooled.connection);
        }
    }

    private static void rollbackQuietly(@Nullable Connection connection) {
        if (connection == null) return;

        try {
            if (!connection.getAutoCommit()) connection.rollback();
        } catch (SQLException e) {
            LOGGER.atWarning().withCause(e).log("Failed to roll a quest storage transaction back");
        }
    }

    private static void closeQuietly(@Nullable Connection connection) {
        if (connection == null) return;

        try {
            connection.close();
        } catch (SQLException ignored) {
            // Closing what is already broken has nothing left to report
        }
    }

    /**
     * Separate from {@link java.util.function.Function} only so the body may throw
     * {@link SQLException}.
     */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(@Nonnull Connection connection) throws SQLException;
    }

    private record Pooled(@Nonnull Connection connection, long idleSince) {

        boolean isUsable() {
            try {
                if (connection.isClosed()) return false;
                if (System.currentTimeMillis() - idleSince < VALIDATE_AFTER_IDLE_MILLIS) return true;

                return connection.isValid(VALIDATION_TIMEOUT_SECONDS);
            } catch (SQLException e) {
                return false;
            }
        }
    }
}
