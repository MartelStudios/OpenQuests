package com.martelstudios.openquests.core.rewards;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.martelstudios.openquests.core.utils.EntityComponents;

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
     * @return {@code false} if it could not be granted right now, e.g. a full inventory
     */
    public abstract boolean grant(@Nonnull EntityComponents playerComponents);

    /**
     * @return whether this reward is handed over the moment it is owed, rather than waiting for the
     * player to come and collect.
     */
    public boolean isAutoClaim() {
        return autoClaim;
    }
}
