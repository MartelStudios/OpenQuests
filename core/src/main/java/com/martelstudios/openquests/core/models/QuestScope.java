package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

/**
 * Who shares a quest beyond its players, written on it by the scope holding it: a world, a group
 * of worlds, the whole server. A quest without one is its players' own. Each kind says how a quest
 * it holds is shared and let go of, so the rest of the core asks it rather than telling them apart.
 */
public abstract class QuestScope {

    /**
     * Polymorphic dispatcher: each kind registers under a {@code "Type"} tag, so a plugin can add
     * its own.
     */
    public static final CodecMapCodec<QuestScope> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<QuestScope> BASE_CODEC = BuilderCodec.abstractBuilder(QuestScope.class).build();

    /**
     * @return whether anything holding the quest is there to share another one with: a world
     * scope whose worlds have all closed is not.
     */
    public abstract boolean isReachable();

    /**
     * @return the ids of the quests the holders of this scope index, those that ended included.
     */
    @Nonnull
    public abstract Collection<UUID> getQuestIds();

    /**
     * Shares another quest the way this one is shared: with the same worlds, the same group or the
     * whole server. The quest is registered already.
     */
    public abstract void share(@Nonnull AbstractQuestProgression<?> quest);

    /**
     * Takes a quest leaving the store for good out of every index of this scope naming it.
     */
    public abstract void release(@Nonnull AbstractQuestProgression<?> quest);
}
