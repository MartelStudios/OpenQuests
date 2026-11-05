package com.martelstudios.openquests.extension.quests.composite;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.martelstudios.openquests.core.models.QuestVisibility;
import com.martelstudios.openquests.extension.hud.QuestHudContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
        CompositeScene scene = new CompositeScene(operator);
        for (QuestVisibility visibility : steps) scene.running(visibility);

        UICommandBuilder builder = new UICommandBuilder();
        new CompositeQuestHudRenderer(scene.steps()).render(new QuestHudContext(builder, UUID.randomUUID()), scene.group);
        return builder;
    }

    private static long appended(UICommandBuilder builder, String document) {
        return Arrays.stream(builder.getCommands())
                     .filter(command -> command.type == CustomUICommandType.Append && document.equals(command.text))
                     .count();
    }
}
