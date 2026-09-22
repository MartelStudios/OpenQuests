package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.hud.QuestHudContext;
import com.martelstudios.openquests.extension.hud.QuestHudRenderer;
import com.martelstudios.openquests.extension.hud.QuestHudRows;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A title carrying the group, then its children one level in. The composite shows no progress of
 * its own: what it is worth is what its children show.
 */
public final class CompositeQuestHudRenderer implements QuestHudRenderer {

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

        context.into(rowSelector + "#SubList", () -> renderChildren(context, composite));
    }

    /**
     * Walks the children by index, since {@code questIds} and the asset ids are built together and
     * a child archived on completion is only nameable through its asset.
     */
    private static void renderChildren(@Nonnull QuestHudContext context, @Nonnull CompositeQuestProgression composite) {
        CompositeQuestAsset asset = composite.getAsset();
        if (asset == null) return;

        UUID[] questIds = composite.getQuestIds();
        String[] assetIds = asset.getAssetIds();

        boolean separated = asset.getOperator() == CompositeQuestAsset.Operator.OR;

        for (int i = 0; i < questIds.length; i++) {
            if (separated && i > 0) context.appendRow(SEPARATOR_DOCUMENT);

            AbstractQuestProgression<?> child = QuestProgressionService.get().getQuest(questIds[i]);

            if (child == null) {
                renderArchivedChild(context, composite, questIds[i], i < assetIds.length ? assetIds[i] : null);
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
    private static void renderArchivedChild(@Nonnull QuestHudContext context, @Nonnull CompositeQuestProgression composite, @Nonnull UUID questId, String assetId) {
        if (assetId == null) return;

        QuestAsset asset = QuestAsset.getAsset(assetId);
        if (asset == null) return;

        QuestState outcome = composite.outcomeOf(questId);

        QuestHudRows.appendRow(context, AbstractQuestProgression.titleOf(asset), outcome == null ? QuestState.SUCCESSFUL : outcome);
    }
}
