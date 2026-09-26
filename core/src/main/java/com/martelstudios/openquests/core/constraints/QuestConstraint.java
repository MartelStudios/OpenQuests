package com.martelstudios.openquests.core.constraints;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A rule an asset lays on the quests made from it, on top of what its type asks for. Composed
 * rather than inherited, so any type of quest can be timed, bound to a world, or both.
 *
 * <p>Every hook answers "no objection" unless a constraint overrides it, so a kind only speaks to
 * the moments it is about.
 */
public abstract class QuestConstraint {

    /**
     * Polymorphic dispatcher: concrete constraint codecs register under a {@code "Type"} tag.
     */
    public static final CodecMapCodec<QuestConstraint> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every constraint; concrete codecs chain from this.
     */
    public static final BuilderCodec<QuestConstraint> BASE_CODEC = BuilderCodec.abstractBuilder(QuestConstraint.class)
                                                                               .build();

    /**
     * Whether what a player just did may count towards the quest. Asked on the thread the action
     * happened on, usually the player's world thread, so a constraint reading their entity checks
     * it is there first.
     *
     * @param actorId the player whose action it was, who may be any of the quest's holders.
     */
    public boolean allowsProgress(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID actorId) {
        return true;
    }

    /**
     * Asked once per asset at boot, so a mistake stops the server rather than surfacing the day a
     * player meets it.
     *
     * @return why the asset carrying this cannot load, {@code null} if it can.
     */
    @Nullable
    public String validate(@Nonnull OpenQuestAsset asset) {
        return null;
    }
}
