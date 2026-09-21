package com.martelstudios.openquests.core.persistence.jdbc;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * The handful of places where portable SQL is not enough. Everything else the backend writes is
 * plain SQL-92, so a database not named here still works through {@link #GENERIC}.
 */
public enum SqlDialect {

    /**
     * {@code ON CONFLICT (…) DO UPDATE SET}, the SQL standard spelling, shared with SQLite and H2.
     */
    POSTGRESQL("postgresql", "TEXT"),
    SQLITE("sqlite", "TEXT"),

    /**
     * Takes {@code ON CONFLICT DO NOTHING} and no further, so it goes the long way round like a
     * database this build has never heard of.
     */
    H2("h2", "TEXT"),

    /**
     * {@code ON DUPLICATE KEY UPDATE}, which names no key: the table's own uniqueness decides.
     * {@code TEXT} tops out at 64 KiB there, which a quest with a large composite would reach.
     */
    MYSQL("mysql", "LONGTEXT"),
    MARIADB("mariadb", "LONGTEXT"),

    /**
     * A database this build has not been told about. Upserts become a delete followed by an
     * insert, which costs a statement and works everywhere.
     */
    GENERIC("", "TEXT");

    private final String urlToken;
    private final String textType;

    SqlDialect(@Nonnull String urlToken, @Nonnull String textType) {
        this.urlToken = urlToken;
        this.textType = textType;
    }

    /**
     * Reads the dialect off {@code jdbc:<vendor>:…}, which is the one thing every JDBC URL agrees
     * on.
     */
    @Nonnull
    public static SqlDialect fromUrl(@Nonnull String url) {
        String lower = url.toLowerCase(Locale.ROOT);

        for (SqlDialect dialect : values()) {
            if (!dialect.urlToken.isEmpty() && lower.startsWith("jdbc:" + dialect.urlToken)) return dialect;
        }
        return GENERIC;
    }

    /**
     * @return the column type for a JSON document. Kept as text rather than a vendor's JSON type
     * so one schema serves every database; PostgreSQL casts it to {@code jsonb} in a query when a
     * server owner wants to search it.
     */
    @Nonnull
    public String getTextType() {
        return textType;
    }

    /**
     * @return {@code false} when an insert has to be preceded by a delete to stand in for an
     * upsert.
     */
    public boolean supportsUpsert() {
        return this != GENERIC && this != H2;
    }

    /**
     * The clause turning an {@code INSERT} into an upsert. Assignments follow it as
     * {@code column = ?} in either spelling, so the values are bound a second time.
     *
     * @param keyColumns the conflicting columns, comma separated. Ignored by MySQL, which goes by
     * whichever key the row collided on.
     */
    @Nonnull
    public String upsertClause(@Nonnull String keyColumns) {
        return switch (this) {
            case MYSQL, MARIADB -> " ON DUPLICATE KEY UPDATE ";
            case GENERIC, H2 -> throw new IllegalStateException("No upsert on " + this);
            default -> " ON CONFLICT (" + keyColumns + ") DO UPDATE SET ";
        };
    }
}
