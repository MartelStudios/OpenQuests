package com.martelstudios.openquests.core.persistence.jdbc;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.persistence.QuestStorageException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ServiceLoader;

/**
 * Finds the JDBC driver, from a jar the server owner dropped somewhere or from the classpath.
 *
 * <p>A driver loaded from a jar of our own is never handed to {@code DriverManager}, which goes by
 * the caller's class loader and would refuse to see it. The {@link Driver} is used directly.
 */
public final class JdbcDriverLoader implements Closeable {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nullable
    private final URLClassLoader loader;

    private final Driver driver;

    private JdbcDriverLoader(@Nullable URLClassLoader loader, @Nonnull Driver driver) {
        this.loader = loader;
        this.driver = driver;
    }

    /**
     * @param driverPath a jar to load the driver from, relative to the server directory, or
     * {@code null} to take it off the classpath.
     * @param driverClass the driver's class name, or {@code null} to let the jar name itself
     * through its service declaration.
     * @throws QuestStorageException if no driver can be found, or none accepts the URL — which is
     * the same mistake seen from either end.
     */
    @Nonnull
    public static JdbcDriverLoader load(@Nonnull String url, @Nullable String driverPath, @Nullable String driverClass) {
        if (driverPath == null || driverPath.isBlank()) {
            return new JdbcDriverLoader(null, fromClasspath(url, driverClass));
        }

        Path jar = Paths.get(driverPath).toAbsolutePath();
        if (!Files.isRegularFile(jar)) {
            throw new QuestStorageException("No JDBC driver jar at " + jar);
        }

        URLClassLoader loader;
        try {
            loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, JdbcDriverLoader.class.getClassLoader());
        } catch (IOException e) {
            throw new QuestStorageException("Failed to open the JDBC driver jar " + jar, e);
        }

        try {
            Driver driver = fromLoader(url, driverClass, loader);
            LOGGER.atInfo().log("Loaded the JDBC driver %s from %s", driver.getClass().getName(), jar);

            return new JdbcDriverLoader(loader, driver);
        } catch (RuntimeException e) {
            close(loader);
            throw e;
        }
    }

    @Nonnull
    public Driver getDriver() {
        return driver;
    }

    /**
     * Lets go of the driver jar. Every connection has to be closed first: they are what still
     * holds classes from it.
     */
    @Override
    public void close() {
        close(loader);
    }

    @Nonnull
    private static Driver fromClasspath(@Nonnull String url, @Nullable String driverClass) {
        if (driverClass != null && !driverClass.isBlank()) {
            return instantiate(driverClass, JdbcDriverLoader.class.getClassLoader());
        }

        try {
            return java.sql.DriverManager.getDriver(url);
        } catch (SQLException e) {
            throw new QuestStorageException(
                "No JDBC driver on the classpath answers to " + url + ". Point \"DriverPath\" at the driver jar.", e);
        }
    }

    /**
     * Asks the jar what it ships before insisting on a class name, so a config naming only the
     * path works for every driver that declares itself the standard way.
     */
    @Nonnull
    private static Driver fromLoader(@Nonnull String url, @Nullable String driverClass, @Nonnull ClassLoader loader) {
        if (driverClass != null && !driverClass.isBlank()) return instantiate(driverClass, loader);

        for (Driver driver : ServiceLoader.load(Driver.class, loader)) {
            try {
                if (driver.acceptsURL(url)) return driver;
            } catch (SQLException ignored) {
                // A driver that cannot even answer that is not the one
            }
        }

        throw new QuestStorageException("The JDBC driver jar declares no driver accepting " + url);
    }

    @Nonnull
    private static Driver instantiate(@Nonnull String driverClass, @Nonnull ClassLoader loader) {
        try {
            return (Driver) Class.forName(driverClass, true, loader).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new QuestStorageException("Failed to instantiate the JDBC driver " + driverClass, e);
        }
    }

    private static void close(@Nullable URLClassLoader loader) {
        if (loader == null) return;

        try {
            loader.close();
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to release the JDBC driver jar");
        }
    }
}
