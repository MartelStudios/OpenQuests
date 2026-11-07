package com.martelstudios.openquests.extension.rewards.effect;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;

import javax.annotation.Nonnull;
import java.time.Duration;

/**
 * The effect's status icon, the one the client shows while it runs, then its name and how long it
 * lasts. Effects carry no translated name, so the name is read from the id.
 */
public final class EffectRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return EffectReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var effectReward = (EffectReward) reward;
        EntityEffect asset = EntityEffect.getAssetMap().getAsset(effectReward.getEffect());

        Message label = Message.translation("openquests.page.reward.effect")
                               .param("effect", effectReward.getEffect().replace('_', ' '));

        String icon = asset == null ? null : asset.getStatusEffectIcon();
        Message duration = null;

        if (effectReward.getDurationSeconds() != null) {
            duration = QuestPageRows.formatDuration(Duration.ofMillis(Math.round(effectReward.getDurationSeconds() * 1000)));
        } else if (asset != null && !asset.isInfinite()) {
            duration = QuestPageRows.formatDuration(Duration.ofMillis(Math.round(asset.getDuration() * 1000.0)));
        }

        QuestPageRows.appendImageLine(context, icon, label, duration);
    }
}
