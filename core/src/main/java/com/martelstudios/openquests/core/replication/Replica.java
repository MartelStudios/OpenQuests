package com.martelstudios.openquests.core.replication;

import javax.annotation.Nonnull;

/**
 * Names this server among those sharing one storage. Each server writes only its own share of a
 * shared quest under this name, so two servers must never run under the same one.
 */
public final class Replica {

    /**
     * The name a server stands under until told otherwise: right for a server alone.
     */
    public static final String DEFAULT_ID = "server";

    private static volatile String localId = DEFAULT_ID;

    private Replica() {}

    /**
     * @return the name this server writes its share under.
     */
    @Nonnull
    public static String localId() {
        return localId;
    }

    /**
     * Set once as the storage starts, before any quest is read or changed.
     */
    public static void setLocalId(@Nonnull String id) {
        localId = id;
    }
}
