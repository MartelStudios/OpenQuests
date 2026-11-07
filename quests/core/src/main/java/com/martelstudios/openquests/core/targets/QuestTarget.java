package com.martelstudios.openquests.core.targets;

import org.joml.Vector3d;

import javax.annotation.Nonnull;

/**
 * Where a {@link TargetedQuest} leads.
 */
public sealed interface QuestTarget {

    /**
     * Whoever carries that character, as OpenDialogues hands it out.
     */
    record Character(@Nonnull String id) implements QuestTarget {}

    /**
     * A spot in the player's world.
     */
    record Location(@Nonnull Vector3d position) implements QuestTarget {}
}
