package com.martelstudios.hyquests.core.rewards;

import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.assets.QuestAsset;
import com.martelstudios.hyquests.core.models.QuestHistoryRecord;

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
     * Grants this reward to one player. Must be all-or-nothing: a partial grant would be handed
     * out twice, since a reward that fails stays pending and is retried.
     *
     * @return {@code false} if it could not be granted right now, e.g. a full inventory
     */
    public abstract boolean grant(@Nonnull QuestHistoryRecord questHistoryRecord, @Nonnull Ref<EntityStore> playerRef);
}
