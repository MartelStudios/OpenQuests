package com.martelstudios.openquests.core.rewards;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Something handed to a player when a quest reaches a terminal state. Which rewards apply is
 * decided by the quest's outcome, so an implementation rarely has to look at the quest itself —
 * it is told which completion it is paying for all the same, since a reward that creates something
 * is the only thing in a position to record where that something came from.
 */
public abstract class QuestReward {

    /**
     * Polymorphic dispatcher: concrete reward codecs register under a {@code "Type"} tag.
     */
    public static final CodecMapCodec<QuestReward> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every reward; concrete codecs chain from this.
     */
    public static final BuilderCodec<QuestReward> BASE_CODEC = BuilderCodec.abstractBuilder(QuestReward.class)
                                                                           .append(new KeyedCodec<>("AutoClaim", Codec.BOOLEAN), (reward, value) -> reward.autoClaim = value, reward -> Boolean.valueOf(reward.autoClaim))
                                                                           .add()
                                                                           .build();

    protected boolean autoClaim;

    /**
     * Grants this reward to one player. Must be all-or-nothing: a partial grant would be handed
     * out twice, since a reward that fails stays pending and is retried.
     *
     * @param sourceQuestId the completion being paid for. Most rewards have no use for it — an
     *                      item is an item — but one that hands a quest over is creating something
     *                      whose provenance nothing else could reconstruct afterwards.
     * @return {@code false} if it could not be granted right now, e.g. a full inventory
     */
    public abstract boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents);

    /**
     * @return whether this reward is handed over the moment it is owed, rather than waiting for the
     * player to come and collect.
     */
    public boolean isAutoClaim() {
        return autoClaim;
    }
}
