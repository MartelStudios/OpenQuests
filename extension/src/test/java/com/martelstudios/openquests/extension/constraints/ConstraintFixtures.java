package com.martelstudios.openquests.extension.constraints;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.persistence.CodecJson;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

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

    @Nonnull
    public static TestQuest questStartedAt(@Nullable Instant startedAt) {
        return new TestQuest(startedAt);
    }

    public static final class TestQuest extends AbstractQuestProgression<TestQuest> {
        TestQuest() {}

        private TestQuest(@Nullable Instant startedAt) {
            this.startedAt = startedAt;
        }
    }

    private static final class TestAsset extends OpenQuestAsset {
        @Override
        public AbstractQuestProgression<?> create() {
            return new TestQuest();
        }
    }
}
