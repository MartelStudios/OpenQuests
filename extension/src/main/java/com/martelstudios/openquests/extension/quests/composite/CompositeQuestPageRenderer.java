package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.journal.QuestMark;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The children are the objectives, one line each, leading to the page that details them. The
 * composite shows no progress of its own: what it is worth is what its children are worth.
 *
 * <p>The one type a document could not replace: a chain has as many lines as it has steps, each
 * leading somewhere else and carrying a mark of its own, and none of that can be written down in
 * advance.
 */
public final class CompositeQuestPageRenderer implements QuestPageRenderer {

    /** Between the children of an OR composite: only one of them has to succeed. */
    private static final String SEPARATOR_DOCUMENT = "OpenQuests/Pages/QuestPageSeparator.ui";

    @Nonnull
    @Override
    public Class<?> getQuestType() {
        return CompositeQuestProgression.class;
    }

    /**
     * A row names the group and says how far through it the player is; the children are listed on
     * the group own card, so a row repeating them would say the same thing twice.
     */
    @Override
    public void render(@Nonnull QuestPageContext context, @Nonnull QuestShape shape, @Nonnull String selector,
                       @Nullable AbstractQuestProgression<?> quest, @Nullable OpenQuestAsset asset) {
        var composite = quest instanceof CompositeQuestProgression progression ? progression : null;

        if (shape == QuestShape.ROW) {
            renderRow(context, composite, asset);
            return;
        }

        if (composite != null && composite.getAsset() != null) {
            QuestPageRows.setProgress(context, selector, tallyOf(context, composite));
            renderSteps(context, composite, composite.getAsset());
            return;
        }

        // With no progression left the asset is the only thing that knows what the chain was made
        // of. No tally: nothing here is being counted, whether the group is behind the player or
        // was never handed to them at all.
        if (asset instanceof CompositeQuestAsset compositeAsset) renderPreview(context, compositeAsset);
    }

    private static void renderRow(@Nonnull QuestPageContext context, @Nullable CompositeQuestProgression composite, @Nullable OpenQuestAsset asset) {
        if (composite != null) {
            QuestPageRows.appendLine(context, composite.getTitle(), tallyOf(context, composite));
            return;
        }

        if (asset != null) QuestPageRows.appendLine(context, AbstractQuestProgression.titleOf(asset));
    }

    /**
     * The children, one line each. A row appends its own line, so the selector it is handed is one
     * it never writes onto.
     */
    private static void renderSteps(@Nonnull QuestPageContext context, @Nonnull CompositeQuestProgression composite, @Nonnull CompositeQuestAsset asset) {
        List<Step> steps = stepsOf(context, composite, asset);
        boolean separated = asset.getOperator() == CompositeQuestAsset.Operator.OR;

        for (int i = 0; i < steps.size(); i++) {
            if (separated) openBranch(context, i == 0);

            Step step = steps.get(i);

            // The group is what knows how a child it can no longer reach went, so it says so
            // before the line asks the journal, which cannot place that child at all
            if (step.child() == null) {
                context.marking(step.mark(), () -> renderChild(context, step.assetId()));
                continue;
            }

            AbstractQuestProgression<?> child = step.child();
            context.listing(child, () -> QuestPageRows.render(context, QuestShape.ROW, "", child, child.getAsset()));
        }
    }

    /**
     * The children, what is left to do before what is behind. A player opening a chain is looking
     * for the step they are on, not for the ones they already cleared.
     *
     * <p>Walked by index, since {@code questIds} and the asset ids are built together and a child
     * that kept no trace of itself is only nameable through its asset.
     */
    @Nonnull
    private static List<Step> stepsOf(@Nonnull QuestPageContext context, @Nonnull CompositeQuestProgression composite, @Nonnull CompositeQuestAsset asset) {
        UUID[] questIds = composite.getChildIds();
        String[] assetIds = asset.getAssetIds();

        List<Step> steps = new ArrayList<>(questIds.length);

        for (int i = 0; i < questIds.length; i++) {
            steps.add(new Step(QuestProgressionService.get().getQuest(questIds[i]),
                i < assetIds.length ? assetIds[i] : null, markOf(context, composite, questIds[i])));
        }

        // Stable, so the order the chain was written in survives inside each half
        steps.sort(Comparator.comparing(Step::finished));

        return steps;
    }

    /**
     * What became of one child: read off the child itself where it is still there to ask, off the
     * group where it is not — a child told to keep no trace leaves nothing but what the group
     * wrote down about it.
     *
     * @return {@link QuestMark#LOST} when neither can say. Not a guess at the likeliest outcome:
     * a step nobody kept was once read as one that succeeded, and a journal saying in green
     * something it does not know is worse than one admitting it lost the thread.
     */
    @Nonnull
    private static QuestMark markOf(@Nonnull QuestPageContext context, @Nonnull CompositeQuestProgression composite, @Nonnull UUID childId) {
        AbstractQuestProgression<?> child = QuestProgressionService.get().getQuest(childId);

        // What it came to for whoever is reading, not for everyone else still on it
        if (child != null) return QuestMark.of(child.getStateFor(context.getViewer()));

        QuestState recorded = composite.outcomeOf(childId);
        return recorded == null ? QuestMark.LOST : QuestMark.of(recorded);
    }

    /**
     * A child with no progression of its own, drawn from its asset and leading to it. Leaving a
     * hole where it was would read as if it had never been asked for, and the line is what lets
     * the player open a step the store no longer holds.
     */
    private static void renderChild(@Nonnull QuestPageContext context, @Nullable String assetId) {
        if (assetId == null) return;

        OpenQuestAsset childAsset = OpenQuestAsset.getAsset(assetId);
        if (childAsset == null) return;

        context.listing(assetId, () -> QuestPageRows.appendLine(context, AbstractQuestProgression.titleOf(childAsset)));
    }

    private static void renderPreview(@Nonnull QuestPageContext context, @Nonnull CompositeQuestAsset composite) {
        String[] assetIds = composite.getAssetIds();
        boolean separated = composite.getOperator() == CompositeQuestAsset.Operator.OR;

        // A group that succeeded through an OR did so on one branch, and the asset never says
        // which: its children are left to the journal rather than each shown as the winner. Every
        // other outcome is shared by all of them, an OR included — a chain given up was given up
        // whichever way the player was going to take.
        boolean undecided = separated && context.getMark() == QuestMark.SUCCESSFUL;

        for (int i = 0; i < assetIds.length; i++) {
            if (separated) openBranch(context, i == 0);

            String childAssetId = assetIds[i];

            if (undecided) context.marking(null, () -> renderChild(context, childAssetId));
            else renderChild(context, childAssetId);
        }
    }

    /**
     * How far through the group the player got. Only a step carried out counts: one that failed and
     * one the chain called off are both over, but neither is progress, and a chain that ended badly
     * reading as full would say the opposite of what happened.
     *
     * <p>A step the group has lost track of counts for nothing either, however likely it is to have
     * succeeded — a tally is read as a fact.
     */
    @Nonnull
    private static String tallyOf(@Nonnull QuestPageContext context, @Nonnull CompositeQuestProgression composite) {
        UUID[] questIds = composite.getChildIds();
        int successful = 0;

        for (UUID childId : questIds) {
            if (markOf(context, composite, childId) == QuestMark.SUCCESSFUL) successful++;
        }
        return successful + "/" + questIds.length;
    }

    /**
     * Opens a side of an OR: the rule above it, and the banding started over so both sides begin on
     * the same shade. Continuing the alternation across the rule would shade one side and not the
     * other, which reads as a difference between them rather than as a break.
     */
    private static void openBranch(@Nonnull QuestPageContext context, boolean first) {
        if (!first) context.appendRow(SEPARATOR_DOCUMENT);

        context.restartStripes();
    }

    /**
     * A child of the group, whether it still has a progression, only the asset that named it, or
     * nothing left but the group's word that it was once asked for.
     */
    private record Step(AbstractQuestProgression<?> child, String assetId, @Nonnull QuestMark mark) {

        /**
         * @return whether this belongs below the line rather than above it. A step nobody kept is
         * behind the player whatever became of it: there is nothing left for them to do about it.
         */
        boolean finished() {
            return mark != QuestMark.IN_PROGRESS;
        }
    }
}
