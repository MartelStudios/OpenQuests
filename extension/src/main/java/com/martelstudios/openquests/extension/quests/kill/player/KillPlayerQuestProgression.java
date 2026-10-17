package com.martelstudios.openquests.extension.quests.kill.player;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KillPlayerQuestProgression extends QuantityQuestProgression<KillPlayerQuestProgression> {

    public static final BuilderCodec<KillPlayerQuestProgression> CODEC = BuilderCodec.builder(KillPlayerQuestProgression.class, KillPlayerQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("PlayerToKill", Codec.STRING), (quest, player) -> quest.playerToKill = player, quest -> quest.playerToKill)
                                                                                     .add()
                                                                                     .build();

    /**
     * Overrides the asset's target for this instance alone.
     */
    @Nullable
    protected String playerToKill;

    @Override
    public KillPlayerQuestAsset getAsset() {
        return (KillPlayerQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's target if one was set on it, the asset's otherwise, {@code null}
     * when any player counts.
     */
    @Nullable
    public String getPlayerToKill() {
        return playerToKill != null ? playerToKill : getAsset().getPlayerToKill();
    }

    protected KillPlayerQuestProgression() {}

    /**
     * Sets up a player kill quest, whose target can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<KillPlayerQuestProgression, Builder> {
        Builder(@Nonnull KillPlayerQuestAsset asset) {
            super(new KillPlayerQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's target for this quest alone.
         */
        public Builder playerToKill(@Nullable String playerToKill) {
            quest().playerToKill = playerToKill;
            return this;
        }
    }

    /**
     * @return {@code true} when the victim is the target, or when no target was set.
     */
    public boolean matchesVictim(@Nonnull PlayerRef victim) {
        String target = getPlayerToKill();
        if (target == null || target.isEmpty()) return true;

        return target.equalsIgnoreCase(victim.getUsername()) || target.equalsIgnoreCase(victim.getUuid().toString());
    }
}
