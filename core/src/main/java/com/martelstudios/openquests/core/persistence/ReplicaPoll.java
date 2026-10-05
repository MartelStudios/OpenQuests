package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What other servers wrote since a reader last looked.
 *
 * @param changed the replicas written since, each with its revision
 * @param ended the outcome claimed for each quest asked about that has ended
 */
public record ReplicaPoll(@Nonnull List<QuestReplica> changed, @Nonnull Map<UUID, QuestState> ended) {

    /**
     * Nothing written since.
     */
    public static final ReplicaPoll NONE = new ReplicaPoll(List.of(), Map.of());
}
