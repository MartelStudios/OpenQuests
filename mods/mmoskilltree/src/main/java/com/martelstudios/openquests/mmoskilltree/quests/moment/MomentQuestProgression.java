package com.martelstudios.openquests.mmoskilltree.quests.moment;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Counts the moments of one kind that match its target and qualifier, each by its amount: the XP it
 * gained, one cast, one bounty.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class MomentQuestProgression<Q extends MomentQuestProgression<Q>> extends QuantityQuestProgression<Q> {

    public static final BuilderCodec<MomentQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(MomentQuestProgression.class, QuantityQuestProgression.BASE_CODEC)
                                                                                      .append(new KeyedCodec<>("Target", Codec.STRING), (quest, target) -> quest.target = target, quest -> quest.target)
                                                                                      .add()
                                                                                      .append(new KeyedCodec<>("Qualifier", Codec.STRING), (quest, qualifier) -> quest.qualifier = qualifier, quest -> quest.qualifier)
                                                                                      .add()
                                                                                      .build();

    /**
     * Override the asset's target and qualifier for this instance alone.
     */
    @Nullable
    private String target;

    @Nullable
    private String qualifier;

    @Override
    public MMOSkillTreeMomentQuestAsset getAsset() {
        return (MMOSkillTreeMomentQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's target if one was set on it, the asset's otherwise, {@code null} for any.
     */
    @Nullable
    public String getTarget() {
        return target != null ? MMOSkillTreeMomentQuestAsset.openOrNull(target) : getAsset().getTarget();
    }

    /**
     * @return this instance's qualifier if one was set on it, the asset's otherwise, {@code null} for any.
     */
    @Nullable
    public String getQualifier() {
        return qualifier != null ? MMOSkillTreeMomentQuestAsset.openOrNull(qualifier) : getAsset().getQualifier();
    }

    /**
     * @return whether a moment about that target, so qualified, counts here. Ids are compared
     * ignoring case, the way MMO Skill Tree compares them.
     */
    public boolean matches(@Nonnull String momentTarget, @Nullable String momentQualifier) {
        String wanted = getTarget();
        if (wanted != null && !wanted.equalsIgnoreCase(momentTarget)) return false;

        String wantedQualifier = getQualifier();
        return wantedQualifier == null || wantedQualifier.equalsIgnoreCase(momentQualifier);
    }

    /**
     * Sets up a moment quest, whose target and qualifier can be overridden for that quest alone.
     */
    public abstract static class Builder<Q extends MomentQuestProgression<Q>, B extends Builder<Q, B>> extends QuantityQuestProgression.Builder<Q, B> {

        protected Builder(@Nonnull Q quest, @Nonnull String assetId) {
            super(quest, assetId);
        }

        /**
         * Overrides the asset's target for this quest alone: the skill, ability or board counted.
         */
        public B target(@Nullable String target) {
            ((MomentQuestProgression<?>) quest()).target = target;
            return self();
        }

        /**
         * Overrides the asset's qualifier for this quest alone, such as a bounty's difficulty.
         */
        public B qualifier(@Nullable String qualifier) {
            ((MomentQuestProgression<?>) quest()).qualifier = qualifier;
            return self();
        }

        /**
         * @return a builder for a moment type with nothing of its own to override.
         */
        @Nonnull
        public static <Q extends MomentQuestProgression<Q>> Builder<Q, ?> of(@Nonnull Q quest, @Nonnull String assetId) {
            return new PlainBuilder<>(quest, assetId);
        }
    }

    private static final class PlainBuilder<Q extends MomentQuestProgression<Q>> extends Builder<Q, PlainBuilder<Q>> {
        private PlainBuilder(@Nonnull Q quest, @Nonnull String assetId) {
            super(quest, assetId);
        }
    }
}
