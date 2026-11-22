package com.martelstudios.openquests.core.targets;

import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Where a {@link TargetedQuest} leads.
 */
public sealed interface QuestTarget {

    /**
     * Whoever carries that character, as OpenDialogues hands it out.
     */
    record Character(@Nonnull String id) implements QuestTarget {}

    /**
     * One NPC alone, among those carrying its character.
     */
    record Npc(@Nonnull UUID npc, @Nonnull String character) implements QuestTarget {}

    /**
     * A spot in the player's world.
     */
    record Location(@Nonnull Vector3d position) implements QuestTarget {}
}
