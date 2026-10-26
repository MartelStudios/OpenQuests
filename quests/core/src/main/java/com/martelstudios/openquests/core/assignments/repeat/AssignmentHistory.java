package com.martelstudios.openquests.core.assignments.repeat;

import com.martelstudios.openquests.core.models.AssignmentRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * What one holder already had of one assignment's quest when an occasion comes: the record written
 * on the last hand-out, and the line earlier ones opened. The line costs a pass over the holder's
 * quests, so it is only read for a repeat that asks, and only once.
 */
public final class AssignmentHistory {

    @Nullable
    private final AssignmentRecord record;

    @Nonnull
    private final String occasion;

    private final boolean timed;

    @Nonnull
    private final Supplier<Line> lineReader;

    @Nullable
    private Line line;

    /**
     * @param occasion the key of the occasion now, as the holder tells hand-outs apart
     * @param timed whether the occasion is a period, which comes back each time the holder is reached
     * @param lineReader reads the line off the holder, called at most once
     */
    public AssignmentHistory(@Nullable AssignmentRecord record, @Nonnull String occasion, boolean timed, @Nonnull Supplier<Line> lineReader) {
        this.record = record;
        this.occasion = occasion;
        this.timed = timed;
        this.lineReader = lineReader;
    }

    /**
     * @return the record the last hand-out wrote, {@code null} if nothing was handed out yet.
     */
    @Nullable
    public AssignmentRecord getRecord() {
        return record;
    }

    /**
     * @return whether the last hand-out was for this very occasion.
     */
    public boolean isHandedAlready() {
        return record != null && occasion.equals(record.getOccasion());
    }

    /**
     * A period is handed out once however often its holder is reached during it; an event, a
     * connection or an entry, is a new occasion every time.
     */
    public boolean isSamePeriod() {
        return timed && isHandedAlready();
    }

    /**
     * The record keeps the last occasion only, so an older one is found on the quests it opened:
     * a player coming back to a world entered before.
     *
     * @return whether this holder was handed the quest on this occasion already.
     */
    public boolean isSeen() {
        return isHandedAlready() || line().openedOnOccasion();
    }

    /**
     * @return whether what an earlier hand-out opened is still running for this holder.
     */
    public boolean isLineRunning() {
        return !line().running().isEmpty();
    }

    /**
     * @return the ids of what earlier hand-outs opened and still runs, which a replacement fails.
     */
    @Nonnull
    public List<UUID> getRunningLine() {
        return line().running();
    }

    @Nonnull
    private Line line() {
        if (line == null) line = lineReader.get();
        return line;
    }

    /**
     * What a holder still has of the quests an assignment opened, chains included.
     *
     * @param openedOnOccasion whether any of them was opened on the occasion now
     * @param running the ids of those still running for the holder
     */
    public record Line(boolean openedOnOccasion, @Nonnull List<UUID> running) {}
}
