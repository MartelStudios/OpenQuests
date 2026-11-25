package com.martelstudios.openquests.extension.quests.wait;

import com.martelstudios.openquests.extension.quests.playertick.PlayerTickQuestProgression;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

/**
 * Counts the time its holders spend in game. Sampled with the movement quests since a tick is the
 * only clock a player carries, but whatever they do counts, standing still included.
 */
public class WaitQuestProgression extends PlayerTickQuestProgression<WaitQuestProgression> {

    public static final BuilderCodec<WaitQuestProgression> CODEC =
        BuilderCodec.builder(WaitQuestProgression.class, WaitQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .documentation("Stores progress toward a waiting duration.")
                    .build();

    /**
     * When a sample last counted, so holders ticking side by side share the time rather than add it.
     */
    @Nullable
    private transient Instant lastCountedAt;

    protected WaitQuestProgression() {}

    @Override
    public WaitQuestAsset getAsset() {
        return (WaitQuestAsset) super.getAsset();
    }

    /**
     * A sample is worth its length, cut down to the time since another holder's last counted: a
     * shared quest runs on one clock, however many play it at once.
     */
    @Override
    protected synchronized double advance(@Nonnull MovementStates states, double metres, double seconds) {
        Instant now = now();
        Instant last = lastCountedAt;
        lastCountedAt = now;

        if (last == null) return seconds;

        double since = Duration.between(last, now).toNanos() / 1e9;
        return Math.max(0, Math.min(seconds, since));
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        return Message.translation("openquests.quest.default.wait").param("quantity", getTargetQuantity());
    }
}
