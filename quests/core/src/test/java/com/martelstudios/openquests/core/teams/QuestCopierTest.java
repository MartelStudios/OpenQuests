package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestCopierTest {

    private final UUID stayer = UUID.randomUUID();
    private final UUID leaver = UUID.randomUUID();

    private QuestHarness harness;
    private QuestProgressionService progression;
    private QuestCopier copier;

    @BeforeAll
    static void registerComposite() {
        AbstractQuestProgression.CODEC.register(CopyComposite.TYPE, CopyComposite.class, CopyComposite.CODEC);
    }

    @BeforeEach
    void open() {
        harness = new QuestHarness();
        progression = harness.services().getQuestProgressionService();
        copier = new QuestCopier(progression);
    }

    @AfterEach
    void close() {
        harness.close();
    }

    @Test
    void aCopyKeepsTheProgressUnderANewId() {
        TestQuestProgression source = counted();

        TestQuestProgression copy = (TestQuestProgression) copier.copyFor(source, leaver);

        assertNotEquals(source.getId(), copy.getId());
        assertEquals(3, copy.getCounter());
        assertEquals(Set.of(leaver), Set.copyOf(copy.getPlayers()));
        // Stored to the millisecond, like any quest read back
        assertEquals(source.getStartedAt().toEpochMilli(), copy.getStartedAt().toEpochMilli());
        assertEquals(null, copy.getScope());
        assertEquals(copy, progression.getQuest(copy.getId()));
        assertTrue(harness.questsOf(leaver).contains(copy.getId()));
        assertEquals(Set.of(stayer, leaver), Set.copyOf(source.getPlayers()));
        assertEquals(3, source.getCounter());
    }

    @Test
    void aCopyOfACompositeOwnsFreshChildren() {
        CopyComposite group = new CopyComposite();
        TestQuestProgression step = new TestQuestProgression();
        group.adopt(step);
        progression.registerQuest(step);
        progression.registerQuest(group);
        group.addPlayer(stayer);

        CopyComposite copy = (CopyComposite) copier.copyFor(group, leaver);

        assertEquals(1, copy.getChildIds().length);
        assertTrue(Collections.disjoint(List.of(copy.getChildIds()), List.of(group.getChildIds())));
        AbstractQuestProgression<?> copiedStep = progression.getQuest(copy.getChildIds()[0]);
        assertEquals(copy.getId(), copiedStep.getParentId());
        assertEquals(Set.of(leaver), Set.copyOf(copiedStep.getPlayers()));
        assertEquals(group.getId(), step.getParentId());
    }

    @Test
    void progressOnTheCopyLeavesTheSourceAlone() {
        TestQuestProgression source = counted();
        TestQuestProgression copy = (TestQuestProgression) copier.copyFor(source, leaver);

        copy.addToCounter(1);

        assertEquals(4, copy.getCounter());
        assertEquals(3, source.getCounter());
    }

    private TestQuestProgression counted() {
        TestQuestProgression source = harness.register();
        source.addPlayer(stayer);
        source.addPlayer(leaver);
        harness.count(source, 3, 5);
        return source;
    }

    /**
     * A composite the codec knows by type, so a copy can be read back.
     */
    public static final class CopyComposite extends AbstractCompositeQuestProgression<CopyComposite> {
        static final String TYPE = "CopyComposite";
        static final BuilderCodec<CopyComposite> CODEC = BuilderCodec.builder(CopyComposite.class, CopyComposite::new, AbstractCompositeQuestProgression.BASE_CODEC)
                                                                     .build();
    }
}
