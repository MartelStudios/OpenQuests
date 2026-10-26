package com.martelstudios.openquests.extension.constraints;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.constraints.AssignmentRequest;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestTally;
import com.martelstudios.openquests.core.persistence.CodecJson;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Stand-ins for an asset and a quest, so a constraint can be put to them without an asset store
 * or a server behind it.
 */
public final class ConstraintFixtures {

    private ConstraintFixtures() {}

    /**
     * Reads a constraint the way an asset would, through its codec, so what is tested is what a
     * pack author writes.
     */
    @Nonnull
    public static <C> C decode(@Nonnull BuilderCodec<C> codec, @Nonnull String json) {
        C constraint = CodecJson.decode(codec, json, "constraint");
        assertNotNull(constraint, "the constraint did not decode: " + json);
        return constraint;
    }

    /**
     * @return {@code null} when the codec refuses the document, which is what a validator does.
     */
    @Nullable
    public static <C> C tryDecode(@Nonnull BuilderCodec<C> codec, @Nonnull String json) {
        return CodecJson.decode(codec, json, "constraint");
    }

    @Nonnull
    public static OpenQuestAsset asset() {
        return new TestAsset();
    }

    /**
     * @return a player standing only on what their record says, nothing of the asset running.
     */
    @Nonnull
    public static QuestTally tally(@Nonnull QuestCompletions completions) {
        return QuestTally.of(completions, UUID.randomUUID(), List.of());
    }

    /**
     * @return a request for a new quest from {@link #asset()}, every asset tallied the same.
     */
    @Nonnull
    public static AssignmentRequest request(@Nonnull QuestTally tally, @Nonnull Instant now) {
        return new AssignmentRequest(asset(), UUID.randomUUID(), now, null, assetId -> tally);
    }

    @Nonnull
    public static AssignmentRequest request(@Nonnull QuestCompletions completions, @Nonnull Instant now) {
        return request(tally(completions), now);
    }

    @Nonnull
    public static TestQuest questStartedAt(@Nullable Instant startedAt) {
        return new TestQuest(startedAt);
    }

    public static final class TestQuest extends AbstractQuestProgression<TestQuest> {
        @Nullable
        private final transient Instant startedAt;

        TestQuest() {
            this(null);
        }

        private TestQuest(@Nullable Instant startedAt) {
            this.startedAt = startedAt;
        }

        @Nullable
        @Override
        public Instant getStartedAt() {
            return startedAt;
        }

        /**
         * Hands the quest to a player without the event a test has no server for.
         */
        @Nonnull
        public TestQuest join(@Nonnull UUID playerId) {
            apply(new AbstractQuestProgression.Join(playerId));
            return this;
        }
    }

    private static final class TestAsset extends OpenQuestAsset {
        @Override
        public AbstractQuestProgression.Builder<?, ?> builder() {
            return AbstractQuestProgression.Builder.of(new TestQuest(), "Test");
        }
    }
}
