package com.martelstudios.openquests.dialogues.conditions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestTally;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Holds unless the delay after the player's last quest from an asset is running: before any such
 * quest and once the delay ran out. Counted from when that quest started or ended.
 */
public class OpenDialoguesQuestCooldownCondition extends DialogueCondition {

    public static final String TYPE = "QuestCooldown";

    public static final BuilderCodec<OpenDialoguesQuestCooldownCondition> CODEC = BuilderCodec.builder(OpenDialoguesQuestCooldownCondition.class, OpenDialoguesQuestCooldownCondition::new, DialogueCondition.BASE_CODEC)
                                                                                .append(new KeyedCodec<>("QuestAssetId", Codec.STRING, true), (condition, assetId) -> condition.questAssetId = assetId, condition -> condition.questAssetId)
                                                                                .addValidator(Validators.nonNull())
                                                                                .addValidatorLate(() -> OpenQuestAsset.VALIDATOR_CACHE.getValidator().late())
                                                                                .add()
                                                                                .append(new KeyedCodec<>("Since", new EnumCodec<>(Since.class)), (condition, since) -> condition.since = since, condition -> condition.since)
                                                                                .add()
                                                                                .append(new KeyedCodec<>("Seconds", Codec.DURATION_SECONDS, true), (condition, duration) -> condition.duration = duration, condition -> condition.duration)
                                                                                .addValidator(Validators.nonNull())
                                                                                .addValidator(Validators.greaterThan(Duration.ZERO))
                                                                                .add()
                                                                                .build();

    /**
     * Which moment of the last quest starts the delay.
     */
    public enum Since {
        START,
        END
    }

    protected String questAssetId;
    @Nonnull
    protected Since since = Since.END;
    protected Duration duration;

    protected OpenDialoguesQuestCooldownCondition() {}

    /**
     * Does not hold without a player, as for a context built for no one.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        if (context.player() == null) return false;
        return !isActive(OpenQuestsServices.installed().getQuestPlayerStateService().getTally(context.player().getUuid(), questAssetId), Instant.now());
    }

    boolean isActive(@Nonnull QuestTally tally, @Nonnull Instant now) {
        Instant last = since == Since.START ? tally.getLastStartedAt() : tally.getLastCompletedAt();
        return last != null && Duration.between(last, now).compareTo(duration) < 0;
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return duration == null || duration.compareTo(Duration.ZERO) <= 0 ? "Seconds must be positive" : null;
    }
}
