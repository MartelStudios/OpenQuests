package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * One server's replica of one quest, as that server last wrote it.
 *
 * @param revision grows with every write of this replica, which is how a reader tells it changed
 * @param quest the replica itself, to be merged into the copy the reader holds
 */
public record QuestReplica(@Nonnull UUID questId, @Nonnull String serverId, long revision, @Nonnull AbstractQuestProgression<?> quest) {}
