package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.assignments.repeat.AssignmentHistory;
import com.martelstudios.openquests.core.assignments.repeat.AssignmentRepeat;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestOrigin;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.SetStateVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Offers one quest of an assignment to one holder: lets the repeat decide on what the holder
 * already had, then carries the decision out. The same for every kind of holder.
 */
public final class AssignmentOffer {

    @Nonnull
    private final Consumer<List<UUID>> failLine;

    public AssignmentOffer() {
        this(ids -> QuestProgressionService.get().progress(new SetStateVisitor(QuestState.FAILED), ids));
    }

    /**
     * @param failLine fails what a replaced hand-out had opened and still runs
     */
    AssignmentOffer(@Nonnull Consumer<List<UUID>> failLine) {
        this.failLine = failLine;
    }

    /**
     * A replaced line fails only once the new quest is out, so a refused hand-out leaves it be. A
     * hand-out another server wrote first is decided once more, on what it wrote.
     *
     * @param occasion the key of the occasion, as that holder tells hand-outs apart
     * @param timed whether the occasion is a period, which comes back each time the holder is reached
     * @return whether the holder now has this occasion handed out, by this call or an earlier one.
     */
    public boolean offer(@Nonnull OpenQuestAssignment assignment, @Nonnull OpenQuestAsset asset, @Nonnull AssignmentHolder holder, @Nonnull String occasion, boolean timed) {
        for (int attempt = 0; ; attempt++) {
            Boolean outcome = tryOffer(assignment, asset, holder, occasion, timed);
            if (outcome != null) return outcome;
            if (attempt > 0 || !holder.refresh()) return false;
        }
    }

    /**
     * @return the outcome of the offer, {@code null} for a hand-out refused, which may be decided
     * once more.
     */
    @Nullable
    private Boolean tryOffer(@Nonnull OpenQuestAssignment assignment, @Nonnull OpenQuestAsset asset, @Nonnull AssignmentHolder holder, @Nonnull String occasion, boolean timed) {
        String assignmentId = assignment.getId();
        String questAssetId = asset.getId();

        AssignmentRecord record = holder.getRecord(assignmentId, questAssetId);
        AssignmentHistory history = new AssignmentHistory(record, occasion, timed, () -> readLine(holder, assignmentId, questAssetId, occasion));

        AssignmentRepeat.Decision decision = assignment.getRepeat().decide(history);
        if (decision == AssignmentRepeat.Decision.SKIP) return history.isHandedAlready();

        // Read before the new quest is out, which would otherwise count as part of the line
        List<UUID> replaced = decision == AssignmentRepeat.Decision.REPLACE ? history.getRunningLine() : List.of();

        AbstractQuestProgression<?> quest = asset.create();
        quest.setOrigin(new QuestOrigin(assignmentId, questAssetId, occasion));

        if (!holder.handOut(quest, assignmentId, questAssetId, record, AssignmentRecord.next(record, occasion, Instant.now()))) return null;

        if (!replaced.isEmpty()) failLine.accept(replaced);
        return true;
    }

    /**
     * Everything one occasion opened carries its origin down the chain, so the line is found on
     * the holder's quests without following any chain.
     */
    @Nonnull
    private static AssignmentHistory.Line readLine(@Nonnull AssignmentHolder holder, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nonnull String occasion) {
        boolean openedOnOccasion = false;
        List<UUID> running = new ArrayList<>();

        for (AbstractQuestProgression<?> held : holder.getQuests()) {
            QuestOrigin origin = held.getOrigin();
            if (origin == null || !origin.isLineOf(assignmentId, questAssetId)) continue;

            if (occasion.equals(origin.getOccasion())) openedOnOccasion = true;
            if (holder.isRunning(held)) running.add(held.getId());
        }
        return new AssignmentHistory.Line(openedOnOccasion, running);
    }
}
