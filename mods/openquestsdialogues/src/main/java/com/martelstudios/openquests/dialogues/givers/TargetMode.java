package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.targets.QuestTarget;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Whom a quest aims at: a fixed character, or the NPC that gave it, by its character or alone.
 */
public enum TargetMode {
    /**
     * The character written on the asset.
     */
    Character,
    /**
     * Every NPC carrying the giver's character.
     */
    GiverCharacter,
    /**
     * The giver NPC alone.
     */
    GiverNpc;

    /** Shared by every field choosing a target. */
    public static final String DOCUMENTATION = "Whom the quest aims at: the Character field, every NPC of the giver's character, or the giver NPC alone, defaulting to Character.";

    /**
     * @param character the asset's fixed character, read by {@link #Character} alone.
     * @return where that quest leads, {@code null} without a character or a giver to aim at.
     */
    @Nullable
    public QuestTarget resolve(@Nullable String character, @Nonnull AbstractQuestProgression<?> quest) {
        if (this == Character) return character == null ? null : new QuestTarget.Character(character);

        Giver giver = Giver.of(quest);
        if (giver == null) return null;
        return this == GiverCharacter ? new QuestTarget.Character(giver.character()) : new QuestTarget.Npc(giver.npc(), giver.character());
    }

    /**
     * @return the character a target leads to, its NPC's for one NPC, {@code null} for none.
     */
    @Nullable
    public static String characterOf(@Nullable QuestTarget target) {
        return switch (target) {
            case QuestTarget.Character character -> character.id();
            case QuestTarget.Npc npc -> npc.character();
            case null, default -> null;
        };
    }

    /**
     * @param characterId the character of the NPC met, {@code null} for none.
     * @param npc         that NPC, {@code null} for none.
     * @return whether that NPC is the one the target leads to.
     */
    public static boolean accepts(@Nullable QuestTarget target, @Nullable String characterId, @Nullable UUID npc) {
        return switch (target) {
            case QuestTarget.Character character -> character.id().equals(characterId);
            case QuestTarget.Npc one -> one.npc().equals(npc);
            case null, default -> false;
        };
    }
}
