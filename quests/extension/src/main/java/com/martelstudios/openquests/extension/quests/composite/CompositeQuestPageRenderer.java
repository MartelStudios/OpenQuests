package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestMark;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    private final CompositeSteps steps;

    public CompositeQuestPageRenderer(@Nonnull CompositeSteps steps) {
        this.steps = steps;
    }

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
            QuestPageRows.setProgress(context, selector, tallyOf(context.getViewer(), composite));
            renderSteps(context, composite, composite.getAsset());
            return;
        }

        // With no progression left the asset is the only thing that knows what the chain was made
        // of. No tally: nothing here is being counted, whether the group is behind the player or
        // was never handed to them at all.
        if (asset instanceof CompositeQuestAsset compositeAsset) renderPreview(context, compositeAsset);
    }

    private void renderRow(@Nonnull QuestPageContext context, @Nullable CompositeQuestProgression composite, @Nullable OpenQuestAsset asset) {
        if (composite != null) {
            QuestPageRows.appendLine(context, composite.getTitle(), tallyOf(context.getViewer(), composite));
            return;
        }

        if (asset != null) QuestPageRows.appendLine(context, AbstractQuestProgression.titleOf(asset));
    }

    /**
     * The children, one line each. A row appends its own line, so the selector it is handed is one
     * it never writes onto.
     */
    private void renderSteps(@Nonnull QuestPageContext context, @Nonnull CompositeQuestProgression composite, @Nonnull CompositeQuestAsset asset) {
        List<CompositeStep> ordered = orderedSteps(context.getViewer(), composite);
        boolean separated = asset.getOperator() == CompositeQuestAsset.Operator.OR;

        for (int i = 0; i < ordered.size(); i++) {
            if (separated) openBranch(context, i == 0);

            CompositeStep step = ordered.get(i);
            AbstractQuestProgression<?> child = step.run();

            // The group is what knows how a child it can no longer reach went, so it says so
            // before the line asks the journal, which cannot place that child at all
            if (child == null) {
                context.marking(step.markFor(context.getViewer()), () -> renderChild(context, step.asset()));
                continue;
            }

            context.listing(child, () -> QuestPageRows.render(context, QuestShape.ROW, "", child, child.getAsset()));
        }
    }

    /**
     * The children worth listing, what is left to do before what is behind. A player opening a
     * chain is looking for the step they are on, not for the ones they already cleared.
     */
    @Nonnull
    private List<CompositeStep> orderedSteps(@Nonnull UUID viewer, @Nonnull CompositeQuestProgression composite) {
        List<CompositeStep> listed = steps.listedIn(composite);

        // Stable, so the order the chain was written in survives inside each half. A step nobody
        // kept is behind the player whatever became of it: nothing is left for them to do about it.
        listed.sort(Comparator.comparing(step -> step.markFor(viewer) != QuestMark.IN_PROGRESS));

        return listed;
    }

    /**
     * A child with no progression of its own, drawn from its asset and leading to it. Leaving a
     * hole where it was would read as if it had never been asked for, and the line is what lets
     * the player open a step the store no longer holds.
     */
    private void renderChild(@Nonnull QuestPageContext context, @Nullable OpenQuestAsset childAsset) {
        if (childAsset == null) return;

        context.listing(childAsset.getId(), () -> QuestPageRows.appendLine(context, AbstractQuestProgression.titleOf(childAsset)));
    }

    private void renderPreview(@Nonnull QuestPageContext context, @Nonnull CompositeQuestAsset composite) {
        List<OpenQuestAsset> previewed = steps.previewedIn(composite);
        boolean separated = composite.getOperator() == CompositeQuestAsset.Operator.OR;

        // A group that succeeded through an OR did so on one branch, and the asset never says
        // which: its children are left to the journal rather than each shown as the winner. Every
        // other outcome is shared by all of them, an OR included: a chain given up was given up
        // whichever way the player was going to take.
        boolean undecided = separated && context.getMark() == QuestMark.SUCCESSFUL;

        for (int i = 0; i < previewed.size(); i++) {
            if (separated) openBranch(context, i == 0);

            OpenQuestAsset childAsset = previewed.get(i);

            if (undecided) context.marking(null, () -> renderChild(context, childAsset));
            else renderChild(context, childAsset);
        }
    }

    /**
     * How far through the group the player got. Only a step carried out counts: one that failed and
     * one the chain called off are both over, but neither is progress, and a chain that ended badly
     * reading as full would say the opposite of what happened.
     *
     * <p>A step the group has lost track of counts for nothing either, however likely it is to have
     * succeeded: a tally is read as a fact. A step kept out of sight is not counted at all.
     */
    @Nonnull
    String tallyOf(@Nonnull UUID viewer, @Nonnull CompositeQuestProgression composite) {
        List<CompositeStep> listed = steps.listedIn(composite);
        long successful = listed.stream().filter(step -> step.markFor(viewer) == QuestMark.SUCCESSFUL).count();

        return successful + "/" + listed.size();
    }

    /**
     * Opens a side of an OR: the rule above it, and the banding started over so both sides begin on
     * the same shade. Continuing the alternation across the rule would shade one side and not the
     * other, which reads as a difference between them rather than as a break.
     */
    private void openBranch(@Nonnull QuestPageContext context, boolean first) {
        if (!first) context.appendRow(SEPARATOR_DOCUMENT);

        context.restartStripes();
    }
}
