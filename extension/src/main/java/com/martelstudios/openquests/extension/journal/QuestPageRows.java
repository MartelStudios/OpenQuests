package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The look of a plain line inside a quest's details. A renderer is free to ignore all of this and
 * append its own document, but one that only has a label and a figure to show gets it from here.
 */
public final class QuestPageRows {
    public static final String ROW_DOCUMENT = "Pages/QuestPageRow.ui";
    public static final String LINE_DOCUMENT = "Pages/QuestPageLineRow.ui";
    public static final String QUEST_LINE_DOCUMENT = "Pages/QuestPageQuestLine.ui";
    public static final String ITEM_LINE_DOCUMENT = "Pages/QuestPageItemLine.ui";

    private static final String COMMON_DOCUMENT = "Pages/QuestPageCommon.ui";

    /** The mark of a detail line, one level in: the column around it keeps the labels aligned. */
    private static final String LINE_ICON = "#IconSlot #Icon";

    /** Kept as references so the shade and the marks stay in the markup, next to the palette. */
    private static final Value<String> STRIPE_BACKGROUND = Value.ref(COMMON_DOCUMENT, "StripeBackground");
    private static final Value<String> TRACKED_BACKGROUND = Value.ref(COMMON_DOCUMENT, "TrackedRowBackground");
    private static final Value<String> ICON_IN_PROGRESS = Value.ref(COMMON_DOCUMENT, "QuestIconDefault");
    private static final Value<String> ICON_COMPLETED = Value.ref(COMMON_DOCUMENT, "QuestIconComplete");
    private static final Value<String> ICON_FAILED = Value.ref(COMMON_DOCUMENT, "QuestIconFailed");
    private static final Value<String> ICON_ABANDONED = Value.ref(COMMON_DOCUMENT, "QuestIconAbandoned");
    private static final Value<String> ICON_LOCKED = Value.ref(COMMON_DOCUMENT, "QuestIconLocked");
    private static final Value<String> ICON_LOST = Value.ref(COMMON_DOCUMENT, "QuestIconLost");
    private static final Value<String> ICON_NEXT = Value.ref(COMMON_DOCUMENT, "QuestIconNext");

    private QuestPageRows() {}

    /**
     * Appends one line carrying nothing but its label.
     *
     * @return the selector of the line, for whatever the caller wants to add to it.
     */
    @Nonnull
    public static String appendLine(@Nonnull QuestPageContext context, @Nonnull Message label) {
        return appendLabel(context, label);
    }

    /**
     * Appends one line: a label on the left, a figure on the right.
     */
    @Nonnull
    public static String appendLine(@Nonnull QuestPageContext context, @Nonnull Message label, @Nullable String value) {
        String lineSelector = appendLabel(context, label);
        if (value != null) context.getBuilder().set(lineSelector + "#Value.Text", value);

        return lineSelector;
    }

    /**
     * Appends one line whose right-hand side is translated rather than counted.
     */
    @Nonnull
    public static String appendLine(@Nonnull QuestPageContext context, @Nonnull Message label, @Nullable Message value) {
        String lineSelector = appendLabel(context, label);
        if (value != null) context.getBuilder().set(lineSelector + "#Value.TextSpans", value);

        return lineSelector;
    }

    /**
     * A line showing an item before naming it, the way the client shows an item everywhere else.
     * Never a link and never marked: a reward is a thing, not a place in the journal.
     *
     * @param itemId what the slot draws, resolved by the client from its own asset store. Null for
     * an item the server itself could not resolve, which leaves the slot empty rather than sending
     * an id the client would fail on too.
     */
    @Nonnull
    public static String appendItemLine(@Nonnull QuestPageContext context, @Nullable String itemId, @Nonnull Message label, @Nullable String value) {
        String lineSelector = context.appendRow(ITEM_LINE_DOCUMENT);

        if (itemId != null) context.getBuilder().set(lineSelector + "#Icon.ItemId", itemId);

        context.getBuilder().set(lineSelector + "#Label.TextSpans", label);

        if (value != null) context.getBuilder().set(lineSelector + "#Value.Text", value);

        stripe(context, lineSelector);

        return lineSelector;
    }

    /**
     * The caret the client puts on a dropdown, borrowed to say that a quest opens onto another.
     */
    public static void setNextIcon(@Nonnull QuestPageContext context, @Nonnull String lineSelector) {
        context.getBuilder().set(lineSelector + LINE_ICON + ".Background", ICON_NEXT);
    }

    /**
     * The label of a line, drawn as a link when the context is pointing at a quest the journal is
     * showing. A renderer never asks for this: it wraps its call in
     * {@link QuestPageContext#linkingTo} and the line turns into a link on its own.
     */
    @Nonnull
    private static String appendLabel(@Nonnull QuestPageContext context, @Nonnull Message label) {
        String target = context.getLinkTarget();
        String lineSelector;

        if (target == null) {
            lineSelector = context.appendRow(LINE_DOCUMENT);
            context.getBuilder().set(lineSelector + "#Label.TextSpans", label);
        } else {
            lineSelector = context.appendRow(QUEST_LINE_DOCUMENT);

            context.getBuilder().set(lineSelector + "#Link.TextSpans", label);
            context.openOnActivating(lineSelector + "#Link", target);
        }

        stripe(context, lineSelector);
        mark(context, lineSelector);

        return lineSelector;
    }

    /**
     * The tracker's diamond, gold while a step is running, green once it succeeded and crossed out
     * once it ended any other way. A line standing for nothing the journal can name keeps its empty
     * slot, so the labels stay in one column.
     */
    private static void mark(@Nonnull QuestPageContext context, @Nonnull String lineSelector) {
        QuestMark mark = context.getMark();
        if (mark == null) return;

        setIcon(context, lineSelector + LINE_ICON, mark);
    }

    /**
     * Puts the mark on a row, wherever the row keeps its icon slot.
     */
    public static void setIcon(@Nonnull QuestPageContext context, @Nonnull String iconSelector, @Nonnull QuestMark mark) {
        context.getBuilder().set(iconSelector + ".Background", iconOf(mark));
    }

    /**
     * Abandoning has a mark of its own rather than borrowing the one for failure: most quests that
     * end this way were called off with the chain around them rather than lost, and reading every
     * step of a chain as a defeat says something about the player that is not true.
     */
    @Nonnull
    private static Value<String> iconOf(@Nonnull QuestMark mark) {
        return switch (mark) {
            case SUCCESSFUL -> ICON_COMPLETED;
            case FAILED -> ICON_FAILED;
            case ABANDONED -> ICON_ABANDONED;
            case LOCKED -> ICON_LOCKED;
            case LOST -> ICON_LOST;
            default -> ICON_IN_PROGRESS;
        };
    }

    /**
     * Shades every other line. A label and its figure sit at opposite ends of a wide row, and the
     * banding is what keeps them on the same line for the reader.
     */
    /**
     * Frames a quest row to say it is on the tracker. The row keeps its own background until then,
     * so the frame is the whole of what tracking looks like.
     */
    public static void setTracked(@Nonnull QuestPageContext context, @Nonnull String rowSelector) {
        // The selector trails a space so a child appends to it. The row itself takes none.
        context.getBuilder().set(rowSelector.stripTrailing() + ".Background", TRACKED_BACKGROUND);
    }

    private static void stripe(@Nonnull QuestPageContext context, @Nonnull String lineSelector) {
        if (!context.nextStripe()) return;

        // The selector trails a space so a child appends to it. The line itself takes none.
        context.getBuilder().set(lineSelector.stripTrailing() + ".Background", STRIPE_BACKGROUND);
    }

    /**
     * The figure shown on a quest's own row, between its title and its state. Legible with the row
     * still folded, which is what makes it the place for a counter.
     */
    public static void setProgress(@Nonnull QuestPageContext context, @Nonnull String rowSelector, @Nonnull String value) {
        context.getBuilder().set(rowSelector + "#Progress.Text", value);
    }

    /**
     * The document the journal appends for a quest of that shape: the type's own when it declared
     * one, and the journal's otherwise.
     */
    @Nonnull
    public static String documentFor(@Nonnull QuestShape shape, @Nullable AbstractQuestProgression<?> quest,
                                     @Nullable QuestAsset asset, @Nonnull String fallback) {
        QuestPageRenderer renderer = resolve(quest, asset);
        if (renderer == null) return fallback;

        String document = renderer.documentFor(shape);
        return document == null ? fallback : document;
    }

    /**
     * Lets a quest say what it is, in whichever shape is being drawn. A type that registered no
     * renderer says nothing — except in a row, where saying nothing would drop the quest out of the
     * list it belongs to, and its title stands in.
     */
    public static void render(@Nonnull QuestPageContext context, @Nonnull QuestShape shape, @Nonnull String selector,
                              @Nullable AbstractQuestProgression<?> quest, @Nullable QuestAsset asset) {
        QuestPageRenderer renderer = resolve(quest, asset);

        if (renderer == null) {
            if (shape == QuestShape.ROW) appendLine(context, titleOf(quest, asset));
            return;
        }
        renderer.render(context, shape, selector, quest, asset);
    }

    /**
     * The renderer of a quest, found through its progression when it has one and through the
     * progression its asset would build when it does not.
     */
    @Nullable
    private static QuestPageRenderer resolve(@Nullable AbstractQuestProgression<?> quest, @Nullable QuestAsset asset) {
        if (quest != null) return QuestPageService.resolve(quest);

        return asset == null ? null : QuestPageService.resolve(asset);
    }

    /**
     * @return what to call a quest that has nothing else to say, from whichever half of it is left.
     */
    @Nonnull
    private static Message titleOf(@Nullable AbstractQuestProgression<?> quest, @Nullable QuestAsset asset) {
        if (quest != null) return quest.getTitle();

        return asset == null ? Message.raw("") : AbstractQuestProgression.titleOf(asset);
    }

    /**
     * Previews a reward through its renderer, or names its type when none is registered — a player
     * seeing "Command" learns more than a player seeing nothing.
     */
    public static void renderReward(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        QuestRewardRenderer renderer = QuestPageService.resolve(reward);

        if (renderer == null) {
            appendLine(context, Message.raw(reward.getClass().getSimpleName()));
            return;
        }
        renderer.renderPreview(context, reward);
    }
}
