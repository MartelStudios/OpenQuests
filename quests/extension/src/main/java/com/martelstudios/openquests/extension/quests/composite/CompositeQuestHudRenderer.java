package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.hud.QuestHudContext;
import com.martelstudios.openquests.extension.hud.QuestHudRenderer;
import com.martelstudios.openquests.extension.hud.QuestHudRows;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Function;

/**
 * A title carrying the group, then its children one level in. The composite shows no progress of
 * its own: what it is worth is what its children show.
 */
public final class CompositeQuestHudRenderer implements QuestHudRenderer {

    private final Function<UUID, AbstractQuestProgression<?>> quests;

    /**
     * @param quests finds a child still in memory, and answers {@code null} for one that is not.
     */
    public CompositeQuestHudRenderer(@Nonnull Function<UUID, AbstractQuestProgression<?>> quests) {
        this.quests = quests;
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
     * Walks the children by index, since {@code questIds} and the asset ids are built together and
     * a child archived on completion is only nameable through its asset. The rule only stands
     * between two lines drawn: an OR with one branch in sight reads as a plain step.
     */
    private void renderChildren(@Nonnull QuestHudContext context, @Nonnull CompositeQuestProgression composite) {
        CompositeQuestAsset asset = composite.getAsset();
        if (asset == null) return;

        UUID[] questIds = composite.getChildIds();
        String[] assetIds = asset.getAssetIds();

        boolean separated = asset.getOperator() == CompositeQuestAsset.Operator.OR;

        boolean drawnOne = false;

        for (int i = 0; i < questIds.length; i++) {
            AbstractQuestProgression<?> child = quests.apply(questIds[i]);
            String assetId = i < assetIds.length ? assetIds[i] : null;

            if (!composite.isStepListed(child, questIds[i], assetId)) continue;

            if (separated && drawnOne) context.appendRow(SEPARATOR_DOCUMENT);
            drawnOne = true;

            if (child == null) {
                renderArchivedChild(context, composite, questIds[i], assetId);
                continue;
            }

            QuestHudRows.render(context, child);
        }
    }

    /**
     * A completed child leaves the store, so nothing is left to draw it. Leaving a hole where it
     * was would read as if it had never been asked for. The group remembers which way it went;
     * read as a success when even it cannot say, which is what a child gone from the store means.
     */
    private void renderArchivedChild(@Nonnull QuestHudContext context, @Nonnull CompositeQuestProgression composite, @Nonnull UUID questId, String assetId) {
        if (assetId == null) return;

        OpenQuestAsset asset = OpenQuestAsset.getAsset(assetId);
        if (asset == null) return;

        QuestState outcome = composite.outcomeOf(questId);

        QuestHudRows.appendRow(context, AbstractQuestProgression.titleOf(asset), outcome == null ? QuestState.SUCCESSFUL : outcome);
    }
}
