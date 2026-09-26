package com.martelstudios.openquests.extension.constraints.time;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A constraint that ends the quest in time, on the outcome its asset picks: failing is the
 * default, succeeding is how "hold out for five minutes" is written.
 */
public abstract class ExpiringConstraint extends QuestConstraint {

    public static final BuilderCodec<ExpiringConstraint> BASE_CODEC = BuilderCodec.abstractBuilder(ExpiringConstraint.class, QuestConstraint.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("OnExpire", new EnumCodec<>(QuestState.class)), (constraint, state) -> constraint.onExpire = state, constraint -> constraint.onExpire)
                                                                                  .add()
                                                                                  .build();

    @Nonnull
    protected QuestState onExpire = QuestState.FAILED;

    @Nonnull
    @Override
    public QuestState getExpiredState() {
        return onExpire;
    }

    @Nullable
    @Override
    public String validate(@Nonnull OpenQuestAsset asset) {
        return onExpire == QuestState.IN_PROGRESS ? "OnExpire must be an outcome, not InProgress" : null;
    }
}
