package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.replication.StoredState;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What other servers wrote since a reader last looked.
 *
 * @param changed the replicas written since, each with its revision
 * @param states where each quest asked about stands, which a copy takes in if it is later news
 */
public record ReplicaPoll(@Nonnull List<QuestReplica> changed, @Nonnull Map<UUID, StoredState> states) {

    /**
     * Nothing written since.
     */
    public static final ReplicaPoll NONE = new ReplicaPoll(List.of(), Map.of());
}
