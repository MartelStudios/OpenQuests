package com.martelstudios.hyquests.rewards;

import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.assets.QuestAsset;

import javax.annotation.Nonnull;
import java.util.UUID;

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
    public abstract void grant(@Nonnull QuestAsset questAsset, @Nonnull Ref<EntityStore> playerRef);
}
