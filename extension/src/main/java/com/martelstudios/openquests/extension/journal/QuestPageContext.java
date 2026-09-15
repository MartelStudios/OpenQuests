package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * What a renderer needs to draw into the journal: the builders, and the only things it cannot work
 * out on its own — which line it is writing, and where that line leads. Neither is its concern to
 * decide: the page opens the container, and whoever lists a quest says where it points.
 */
public final class QuestPageContext {
    public static final String ROOT_CONTAINER = "#QuestList";

    private final UICommandBuilder builder;
    private final UIEventBuilder eventBuilder;

    /**
     * What the journal knows about the quests a renderer names, so a link is only drawn when it
     * leads somewhere and a line only marked when something can be said. A quest the player never
     * received still leads somewhere: it is read from its asset, which is how a chain can be
     * walked before it is walked.
     */
    private final QuestLookup lookup;

    /**
     * Who is reading. A quest several players share can have ended for this one and still be
     * running for the others, so what it is worth is only ever asked on their behalf.
     */
    private final UUID viewer;

    private String container = ROOT_CONTAINER;
    private int rowCount;

    /** Counts only the lines that take a stripe, so a rule between them keeps the alternation. */
    private int stripeCount;

    @Nullable
    private String linkTarget;

    /**
     * The completion whose rewards are being drawn, or {@code null} outside of a reward list. What
     * a reward hands over is only a fact for the run that paid it; everywhere else it is a promise.
     */
    @Nullable
    private String payingQuestId;

    /**
     * What the lines being drawn are worth. Null when nothing can be said about them at all: a
     * reward preview, or a step of an OR group that succeeded, where neither the asset nor the
     * group can say which branch was taken.
     */
    @Nullable
    private QuestMark mark;

    QuestPageContext(@Nonnull UICommandBuilder builder, @Nonnull UIEventBuilder eventBuilder, @Nonnull QuestLookup lookup, @Nonnull UUID viewer) {
        this.builder = builder;
        this.eventBuilder = eventBuilder;
        this.lookup = lookup;
        this.viewer = viewer;
    }

    /**
     * @return the player this page is being drawn for, for a renderer that has to ask a quest what
     * it came to rather than what it is.
     */
    @Nonnull
    public UUID getViewer() {
        return viewer;
    }

    @Nonnull
    public UICommandBuilder getBuilder() {
        return builder;
    }

    @Nonnull
    public UIEventBuilder getEventBuilder() {
        return eventBuilder;
    }

    /**
     * Appends a line to the container currently being filled.
     *
     * @return the selector of the new line, ending with a space so a child selector appends to it.
     */
    @Nonnull
    public String appendRow(@Nonnull String documentPath) {
        builder.append(container, documentPath);

        return container + "[" + rowCount++ + "] ";
    }

    /**
     * Draws into another container. Whatever the body appends lands there, so the page decides
     * where a renderer's lines go without telling it how to draw.
     *
     * @return how many lines the body appended, so a heading is only shown over something.
     */
    public int into(@Nonnull String containerSelector, @Nonnull Runnable body) {
        String previousContainer = container;
        int previousRowCount = rowCount;
        int previousStripeCount = stripeCount;

        container = containerSelector;
        rowCount = 0;
        stripeCount = 0;
        try {
            body.run();
            return rowCount;
        } finally {
            container = previousContainer;
            rowCount = previousRowCount;
            stripeCount = previousStripeCount;
        }
    }

    /**
     * @return whether the line about to be drawn takes the shaded background. Every other one
     * does, starting with the second, so a lone line is never singled out.
     */
    boolean nextStripe() {
        return stripeCount++ % 2 == 1;
    }

    /**
     * Starts the banding over, so what follows opens on the page's own background. A list broken by
     * a rule is several lists rather than one: each part reads better starting from the same shade
     * than carrying the alternation across the break.
     */
    public void restartStripes() {
        stripeCount = 0;
    }

    /**
     * Makes whatever the body draws lead to a quest. A renderer listing another quest wraps its
     * call in this and stays out of it: the lines it draws become links on their own, the same way
     * {@link #into} places them without their knowing where.
     *
     * @param target a quest id or an asset id, the way the quest commands take either
     */
    public void linkingTo(@Nonnull String target, @Nonnull Runnable body) {
        String previousTarget = linkTarget;

        linkTarget = target;
        try {
            body.run();
        } finally {
            linkTarget = previousTarget;
        }
    }

    /**
     * Marks whatever the body draws as belonging to a quest: its lines lead to that quest's row
     * and carry its state. The one call a renderer listing another quest needs, and the reason
     * none of them has to think about either.
     */
    public void listing(@Nonnull AbstractQuestProgression<?> quest, @Nonnull Runnable body) {
        listing(quest.getId().toString(), QuestMark.of(quest.getStateFor(viewer)), body);
    }

    /**
     * The same for a quest named rather than held: its lines lead to it and carry what the journal
     * knows about it.
     *
     * <p>When the journal knows nothing — never handed out, or ended and kept no record of its own
     * — the lines keep the mark they already had, which is the one the quest above them carries. A
     * chain given up is a chain whose every step was given up, and saying so is nearer the truth
     * than showing a step the player did reach as one they never opened.
     */
    public void listing(@Nonnull String target, @Nonnull Runnable body) {
        QuestMark known = lookup.markOf(target);

        listing(target, known != null ? known : mark, body);
    }

    private void listing(@Nonnull String target, @Nullable QuestMark outcome, @Nonnull Runnable body) {
        String previousTarget = linkTarget;
        QuestMark previousMark = mark;

        linkTarget = target;
        mark = outcome;
        try {
            body.run();
        } finally {
            linkTarget = previousTarget;
            mark = previousMark;
        }
    }

    /**
     * Says which completion the rewards being drawn belong to, so a reward that hands a quest over
     * can point at the one it handed over rather than at its asset. Pass {@code null} for a quest
     * the player does not hold: nothing was paid, so nothing was created.
     */
    public void paying(@Nullable String questId, @Nonnull Runnable body) {
        String previousPaying = payingQuestId;

        payingQuestId = questId;
        try {
            body.run();
        } finally {
            payingQuestId = previousPaying;
        }
    }

    /**
     * Makes whatever the body draws lead to what one reward hands over: the quest this very
     * completion created where there is one, and the asset itself where there is none.
     *
     * <p>Handed over nothing is said as {@link QuestMark#LOCKED} rather than left unsaid, and that
     * is the whole point of the call. A quest that failed paid none of what success would have
     * paid, yet the row still lists it — so without a word the line would go looking for the asset
     * by name and answer with somebody else's run of it, which reads as a quest the player was
     * given and walked away from.
     */
    public void granting(@Nonnull String assetId, @Nonnull Runnable body) {
        String granted = payingQuestId == null ? null : lookup.grantedFrom(payingQuestId, assetId);

        if (granted != null) {
            listing(granted, body);
            return;
        }
        listing(assetId, QuestMark.LOCKED, body);
    }

    /**
     * Marks whatever the body draws, for lines the caller knows better than the journal does. Pass
     * {@code null} to state nothing, which leaves them unmarked rather than guessing.
     */
    public void marking(@Nullable QuestMark outcome, @Nonnull Runnable body) {
        QuestMark previousMark = mark;

        mark = outcome;
        try {
            body.run();
        } finally {
            mark = previousMark;
        }
    }

    /**
     * @return what the line being drawn is worth, or {@code null} when nothing can be said about
     * it and the line takes no mark.
     */
    @Nullable
    public QuestMark getMark() {
        return mark;
    }

    /**
     * @return where the line being drawn should lead, or {@code null} when it names nothing the
     * journal can open. A link that goes nowhere reads as a broken one.
     */
    @Nullable
    String getLinkTarget() {
        return linkTarget != null && lookup.canOpen(linkTarget) ? linkTarget : null;
    }

    /**
     * Opens the quest {@code target} names when the element is pressed. The interface is not
     * locked: the answer is a page the player asked for, not a decision.
     *
     * <p>Whatever mark the line carries goes along with the click. A quest that ended and left
     * nothing behind — a step of a chain, which by default keeps no history of its own — is known
     * to nothing but the line that just drew it, so the page it opens is told rather than left to
     * read it as one never started.
     */
    void openOnActivating(@Nonnull String selector, @Nonnull String target) {
        EventData data = EventData.of(QuestPage.QuestPageEventData.KEY_ACTION, QuestPage.QuestPageEventData.ACTION_OPEN)
                                  .append(QuestPage.QuestPageEventData.KEY_TARGET, target);

        if (mark != null) data.append(QuestPage.QuestPageEventData.KEY_MARK, mark);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, data, false);
    }
}
