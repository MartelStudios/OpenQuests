package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tree the core keeps between a composite and its steps, off a running server: nothing here
 * looks a quest up in the store.
 */
class QuestTreeTest {

    @Test
    void adoptingAStepListsItAndTellsItWhoseStepItIs() {
        TestComposite group = new TestComposite();
        TestQuestProgression step = new TestQuestProgression();

        group.adopt(step);

        assertEquals(group.getId(), step.getParentId());
        assertArrayEquals(new UUID[]{step.getId()}, group.getChildIds());
        assertTrue(step.hasChanges());
    }

    @Test
    void aStepIsListedOnceHoweverOftenItIsAdopted() {
        TestComposite group = new TestComposite();
        TestQuestProgression step = new TestQuestProgression();

        group.adopt(step).adopt(step);

        assertEquals(1, group.getChildIds().length);
    }

    @Test
    void aStepHasOneParent() {
        TestComposite first = new TestComposite();
        TestComposite second = new TestComposite();
        TestQuestProgression step = new TestQuestProgression();

        first.adopt(step);

        assertThrows(IllegalStateException.class, () -> second.adopt(step));
        assertEquals(first.getId(), step.getParentId());
        assertEquals(0, second.getChildIds().length);
    }

    @Test
    void theTreeSurvivesBeingWrittenDown() {
        TestComposite group = new TestComposite();
        TestQuestProgression step = new TestQuestProgression();
        group.adopt(step);

        TestComposite readGroup = CodecJson.decode(TestComposite.CODEC, CodecJson.encode(TestComposite.CODEC, group), "group");
        TestQuestProgression readStep = CodecJson.decode(TestQuestProgression.CODEC, CodecJson.encode(TestQuestProgression.CODEC, step), "step");

        assertNotNull(readGroup);
        assertNotNull(readStep);
        assertEquals(List.of(step.getId()), List.of(readGroup.getChildIds()));
        assertEquals(group.getId(), readStep.getParentId());
        assertNull(readGroup.getParentId());
    }

    /**
     * The smallest composite there is: a tree and nothing it makes of it.
     */
    static final class TestComposite extends AbstractCompositeQuestProgression<TestComposite> {
        static final BuilderCodec<TestComposite> CODEC = BuilderCodec.builder(TestComposite.class, TestComposite::new, AbstractCompositeQuestProgression.BASE_CODEC)
                                                                     .build();
    }
}
