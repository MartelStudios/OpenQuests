package com.martelstudios.hyquests.rewards;

import com.hypixel.hytale.codec.lookup.CodecMapCodec;

import javax.annotation.Nonnull;

/**
 * Something handed to a player when a quest reaches a terminal state. Which rewards apply is
 * decided by the quest's outcome, so an implementation never has to look at the quest itself.
 */
public abstract class QuestReward {

    /**
     * Polymorphic dispatcher: concrete reward codecs register under a {@code "Type"} tag.
     */
    public static final CodecMapCodec<QuestReward> CODEC = new CodecMapCodec<>("Type");

    /**
     * Grants this reward. Called once per player, and never twice for the same completion.
     */
    public abstract void grant(@Nonnull QuestRewardContext context);
}
