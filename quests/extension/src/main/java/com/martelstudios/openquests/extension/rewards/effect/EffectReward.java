package com.martelstudios.openquests.extension.rewards.effect;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Puts one of the game's entity effects on the player, a food buff or a potion, for the effect's
 * own duration unless {@code DurationSeconds} replaces it.
 */
public class EffectReward extends QuestReward {

    public static final BuilderCodec<EffectReward> CODEC = BuilderCodec.builder(EffectReward.class, EffectReward::new, QuestReward.BASE_CODEC)
                                                                       .append(new KeyedCodec<>("Effect", Codec.STRING, true), (reward, effect) -> reward.effect = effect, reward -> reward.effect)
                                                                       .addValidator(Validators.nonNull())
                                                                       .addValidatorLate(() -> EntityEffect.VALIDATOR_CACHE.getValidator().late())
                                                                       .add()
                                                                       .append(new KeyedCodec<>("DurationSeconds", Codec.DOUBLE), (reward, seconds) -> reward.durationSeconds = seconds, reward -> reward.durationSeconds)
                                                                       .addValidator(Validators.greaterThan(0.0))
                                                                       .add()
                                                                       .append(new KeyedCodec<>("OverlapBehavior", OverlapBehavior.CODEC), (reward, overlap) -> reward.overlapBehavior = overlap, reward -> reward.overlapBehavior)
                                                                       .add()
                                                                       .build();

    protected String effect;

    /**
     * {@code null} for the effect's own duration.
     */
    @Nullable
    protected Double durationSeconds;

    /**
     * {@code null} for the effect's own behavior when the player already has it.
     */
    @Nullable
    protected OverlapBehavior overlapBehavior;

    private EffectReward() {}

    /**
     * Left owed for a player out of world, who has no entity to carry the effect, and for one the
     * effect refuses, such as an effect that ignores overlaps and is still running.
     */
    @Override
    public boolean grant(@Nonnull OpenQuestsServices services, @Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        Ref<EntityStore> reference = playerComponents.getReference();
        if (reference == null || !reference.isValid()) return false;

        EntityEffect asset = EntityEffect.getAssetMap().getAsset(effect);
        EffectControllerComponent controller = playerComponents.getComponent(EffectControllerComponent.getComponentType());
        if (asset == null || controller == null) return false;

        if (durationSeconds == null && overlapBehavior == null) return controller.addEffect(reference, asset, reference.getStore());

        float duration = durationSeconds == null ? asset.getDuration() : durationSeconds.floatValue();
        OverlapBehavior overlap = overlapBehavior == null ? asset.getOverlapBehavior() : overlapBehavior;

        return controller.addEffect(reference, asset, duration, overlap, reference.getStore());
    }

    /**
     * @return the id of the effect put on the player.
     */
    @Nonnull
    public String getEffect() {
        return effect;
    }

    /**
     * @return the duration in seconds replacing the effect's own, {@code null} to keep it.
     */
    @Nullable
    public Double getDurationSeconds() {
        return durationSeconds;
    }
}
