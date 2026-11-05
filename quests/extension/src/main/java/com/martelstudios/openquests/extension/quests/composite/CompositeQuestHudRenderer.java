package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.hud.QuestHudContext;
import com.martelstudios.openquests.extension.hud.QuestHudRenderer;
import com.martelstudios.openquests.extension.hud.QuestHudRows;

import javax.annotation.Nonnull;

/**
 * A title carrying the group, then its children one level in. The composite shows no progress of
 * its own: what it is worth is what its children show.
 */
public final class CompositeQuestHudRenderer implements QuestHudRenderer {

    private final CompositeSteps steps;

    public CompositeQuestHudRenderer(@Nonnull CompositeSteps steps) {
        this.steps = steps;
    }

    /** Carries the composite line and, under it, the container its children draw into. */
    private static final String GROUP_DOCUMENT = "OpenQuests/Hud/QuestTrackerGroup.ui";

    /** Between the children of an OR composite: only one of them has to succeed. */
    private static final String SEPARATOR_DOCUMENT = "OpenQuests/Hud/QuestTrackerSeparator.ui";

    @Nonnull
    @Override
    public Class<?> getQuestType() {
        return CompositeQuestProgression.class;
    }

    @Override
    public void render(@Nonnull QuestHudContext context, @Nonnull AbstractQuestProgression<?> quest) {
        var composite = (CompositeQuestProgression) quest;

        String rowSelector = QuestHudRows.appendRow(context, GROUP_DOCUMENT, quest.getTitle(), quest.getState());
        QuestHudRows.appendDescription(context, rowSelector, quest);
        QuestHudRows.appendTimer(context, rowSelector, quest);

        context.into(rowSelector + "#SubList", () -> renderChildren(context, composite));
    }

    /**
     * The rule only stands between two lines drawn: an OR with one branch in sight reads as a plain
     * step.
     */
    private void renderChildren(@Nonnull QuestHudContext context, @Nonnull CompositeQuestProgression composite) {
        CompositeQuestAsset asset = composite.getAsset();
        if (asset == null) return;

        boolean separated = asset.getOperator() == CompositeQuestAsset.Operator.OR;
        boolean drawnOne = false;

        for (CompositeStep step : steps.listedIn(composite)) {
            if (separated && drawnOne) context.appendRow(SEPARATOR_DOCUMENT);
            drawnOne = true;

            if (step.run() != null) QuestHudRows.render(context, step.run());
            else renderArchivedChild(context, step);
        }
    }

    /**
     * A completed child leaves the store, so nothing is left to draw it. Leaving a hole where it
     * was would read as if it had never been asked for. The group remembers which way it went;
     * read as a success when even it cannot say, which is what a child gone from the store means.
     */
    private void renderArchivedChild(@Nonnull QuestHudContext context, @Nonnull CompositeStep step) {
        if (step.asset() == null) return;

        QuestHudRows.appendRow(context, AbstractQuestProgression.titleOf(step.asset()), step.recorded() == null ? QuestState.SUCCESSFUL : step.recorded());
    }
}
