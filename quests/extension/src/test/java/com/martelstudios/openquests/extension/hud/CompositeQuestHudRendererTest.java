package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestVisibility;
import com.martelstudios.openquests.extension.quests.composite.CompositeQuestAsset;
import com.martelstudios.openquests.extension.quests.composite.CompositeQuestHudRenderer;
import com.martelstudios.openquests.extension.quests.composite.CompositeQuestProgression;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What the tracker draws for a composite whose steps are not all worth showing.
 */
class CompositeQuestHudRendererTest {

    private static final String ROW = "OpenQuests/Hud/QuestTrackerRow.ui";
    private static final String SEPARATOR = "OpenQuests/Hud/QuestTrackerSeparator.ui";

    @Test
    void aHiddenOrBranchDrawsNeitherItsLineNorTheRule() {
        UICommandBuilder builder = render(CompositeQuestAsset.Operator.OR, QuestVisibility.ALWAYS, QuestVisibility.NEVER);

        assertEquals(1, appended(builder, ROW));
        assertEquals(0, appended(builder, SEPARATOR));
    }

    @Test
    void aHiddenFirstBranchLeavesNoRuleAboveTheOtherOne() {
        UICommandBuilder builder = render(CompositeQuestAsset.Operator.OR, QuestVisibility.NEVER, QuestVisibility.ALWAYS);

        assertEquals(1, appended(builder, ROW));
        assertEquals(0, appended(builder, SEPARATOR));
    }

    @Test
    void twoVisibleBranchesKeepTheirRule() {
        UICommandBuilder builder = render(CompositeQuestAsset.Operator.OR, QuestVisibility.ALWAYS, QuestVisibility.ALWAYS);

        assertEquals(2, appended(builder, ROW));
        assertEquals(1, appended(builder, SEPARATOR));
    }

    @Test
    void anAndHidesItsHiddenStepToo() {
        UICommandBuilder builder = render(CompositeQuestAsset.Operator.AND, QuestVisibility.NEVER, QuestVisibility.ALWAYS);

        assertEquals(1, appended(builder, ROW));
    }

    private static UICommandBuilder render(CompositeQuestAsset.Operator operator, QuestVisibility... steps) {
        Map<UUID, AbstractQuestProgression<?>> quests = new HashMap<>();
        Step group = new Step(operator, QuestVisibility.ALWAYS, steps.length);

        for (QuestVisibility visibility : steps) {
            Step step = new Step(CompositeQuestAsset.Operator.AND, visibility, 0);
            group.adopt(step);
            quests.put(step.getId(), step);
        }

        UICommandBuilder builder = new UICommandBuilder();
        new CompositeQuestHudRenderer(quests::get).render(new QuestHudContext(builder, UUID.randomUUID()), group);
        return builder;
    }

    private static long appended(UICommandBuilder builder, String document) {
        return Arrays.stream(builder.getCommands())
                     .filter(command -> command.type == CustomUICommandType.Append && document.equals(command.text))
                     .count();
    }

    /**
     * A composite off a server: no asset store behind it, so it carries the asset it reads. No
     * renderer is registered in a test, so a step draws as a plain row.
     */
    private static final class Step extends CompositeQuestProgression {
        private final TestAsset asset;
        private final QuestVisibility visibility;

        Step(CompositeQuestAsset.Operator operator, QuestVisibility visibility, int stepCount) {
            this.asset = new TestAsset(operator, stepCount);
            this.visibility = visibility;
        }

        @Override
        public QuestVisibility getVisibility() {
            return visibility;
        }

        @Override
        public CompositeQuestAsset getAsset() {
            return asset;
        }
    }

    private static final class TestAsset extends CompositeQuestAsset {
        TestAsset(Operator operator, int stepCount) {
            this.operator = operator;
            this.assetIds = new String[stepCount];
        }
    }
}
