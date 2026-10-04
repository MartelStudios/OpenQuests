package com.martelstudios.openquests.core.persistence;

import javax.annotation.Nonnull;

/**
 * Everything the quest system keeps between restarts, and the only thing the rest of the core
 * knows about where it is kept. A backend answers for each kind of record through a port of its
 * own, so a caller depends on the one it uses: the progressions, the indexes of running quests,
 * the players and their messages, the assignment records.
 *
 * <p>Several servers may share one backend. Every record has one writer, or is written through an
 * operation that cannot lose what another server wrote: a replica per server, a member added or
 * removed, a message left, a conditional claim.
 *
 * <p>Reads block, and writes are durable once they return. They are meant for a thread of their
 * own, never a world's, but for a player connecting and a world loading.
 */
public interface QuestStorage extends ProgressionStore, IndexStore, PlayerStore, AssignmentStore, AutoCloseable {

    /**
     * Opens the backend and brings it up to the shape this version expects.
     *
     * @throws QuestStorageException if it cannot be reached or prepared, which stops the server
     * rather than run it against storage nobody can write to.
     */
    void start();

    /**
     * Closes the backend. Whatever is still buffered is written first.
     */
    @Override
    void close();

    /**
     * @return a short name for logs, such as {@code Disk} or {@code Jdbc}.
     */
    @Nonnull
    String getId();

    /**
     * @return the name this server writes its replicas under, unique among the servers sharing
     * the backend.
     */
    @Nonnull
    String getReplicaId();

    /**
     * @return whether other servers may write the backend at the same time, which is what makes
     * looking for their writes worth it.
     */
    boolean isShared();
}
