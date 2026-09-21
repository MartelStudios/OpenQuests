package com.martelstudios.openquests.core.persistence.jdbc;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.QuestStorageProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * {@code {"Type": "Jdbc", "Url": "jdbc:postgresql://…"}} and the handful of things a connection
 * needs beyond it.
 *
 * <p>No driver is shipped: a server owner drops the one their database wants beside the server and
 * names it under {@code "DriverPath"}. That keeps the mod to one jar whatever database it ends up
 * talking to, and lets a driver be updated without waiting for a release.
 */
public class JdbcQuestStorageProvider implements QuestStorageProvider {

    public static final BuilderCodec<JdbcQuestStorageProvider> CODEC = BuilderCodec.builder(JdbcQuestStorageProvider.class, JdbcQuestStorageProvider::new)
                                                                                   .append(new KeyedCodec<>("Url", Codec.STRING), (provider, url) -> provider.url = url, provider -> provider.url)
                                                                                   .addValidator(Validators.nonNull())
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("User", Codec.STRING), (provider, user) -> provider.user = user, provider -> provider.user)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("Password", Codec.STRING), (provider, password) -> provider.password = password, provider -> provider.password)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("DriverClass", Codec.STRING), (provider, driverClass) -> provider.driverClass = driverClass, provider -> provider.driverClass)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("DriverPath", Codec.STRING), (provider, driverPath) -> provider.driverPath = driverPath, provider -> provider.driverPath)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("TablePrefix", Codec.STRING), (provider, prefix) -> provider.tablePrefix = prefix, provider -> provider.tablePrefix)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("PoolSize", Codec.INTEGER), (provider, size) -> provider.poolSize = size, provider -> Integer.valueOf(provider.poolSize))
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("ConnectionTimeoutSeconds", Codec.INTEGER), (provider, seconds) -> provider.connectionTimeoutSeconds = seconds, provider -> Integer.valueOf(provider.connectionTimeoutSeconds))
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("CreateSchema", Codec.BOOLEAN), (provider, create) -> provider.createSchema = create, provider -> Boolean.valueOf(provider.createSchema))
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("ServerId", Codec.STRING), (provider, serverId) -> provider.serverId = serverId, provider -> provider.serverId)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("Dialect", Codec.STRING), (provider, dialect) -> provider.dialect = dialect, provider -> provider.dialect)
                                                                                   .add()
                                                                                   .build();

    private String url;

    @Nullable
    private String user;

    @Nullable
    private String password;

    /**
     * Left out for a driver jar that declares itself, which every current one does.
     */
    @Nullable
    private String driverClass;

    /**
     * Relative to the server directory. Left out for a driver already on the classpath.
     */
    @Nullable
    private String driverPath;

    private String tablePrefix = "openquests_";

    private int poolSize = 8;

    private int connectionTimeoutSeconds = 10;

    /**
     * Turn off on a database whose schema is managed elsewhere — a migration tool, a DBA.
     */
    private boolean createSchema = true;

    /**
     * Written into {@code updated_by}, and the only thing telling one server's writes from
     * another's on a shared database.
     */
    private String serverId = "server";

    /**
     * Overrides what the URL says, for a database reached through a proxy that borrows another
     * vendor's URL scheme. {@code null} to go by the URL.
     */
    @Nullable
    private String dialect;

    @Nonnull
    @Override
    public QuestStorage create() {
        SqlDialect resolved = dialect == null || dialect.isBlank()
            ? SqlDialect.fromUrl(url)
            : SqlDialect.valueOf(dialect.toUpperCase(java.util.Locale.ROOT));

        return new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(
            url, user, password, driverClass, driverPath, tablePrefix,
            poolSize, connectionTimeoutSeconds, createSchema, serverId, resolved));
    }

    @Nonnull
    @Override
    public String toString() {
        return "Jdbc{url='" + url + "', prefix='" + tablePrefix + "', serverId='" + serverId + "'}";
    }
}
