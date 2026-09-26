package com.martelstudios.openquests.extension.constraints.time;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestCompletions;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.UUID;

/**
 * Ends every quest made from the asset at one moment, whenever it started: the close of an event.
 * Written as an ISO-8601 instant, {@code "2026-12-31T23:00:00Z"}.
 */
public class DeadlineConstraint extends ExpiringConstraint {

    public static final BuilderCodec<DeadlineConstraint> CODEC = BuilderCodec.builder(DeadlineConstraint.class, DeadlineConstraint::new, ExpiringConstraint.BASE_CODEC)
                                                                             .append(new KeyedCodec<>("At", Codec.INSTANT, true), (constraint, at) -> constraint.at = at, constraint -> constraint.at)
                                                                             .addValidator(Validators.nonNull())
                                                                             .add()
                                                                             .build();

    protected Instant at;

    protected DeadlineConstraint() {}

    /**
     * Nothing is handed out once the moment has passed: it would only end a few seconds later.
     */
    @Override
    public boolean allowsAssignment(@Nonnull OpenQuestAsset asset, @Nonnull UUID playerId, @Nonnull QuestCompletions completions, @Nonnull Instant now) {
        return now.isBefore(at);
    }

    @Nonnull
    @Override
    public Instant getDeadline(@Nonnull AbstractQuestProgression<?> quest) {
        return at;
    }

    /**
     * @return the moment every quest from the asset ends.
     */
    @Nonnull
    public Instant getAt() {
        return at;
    }
}
