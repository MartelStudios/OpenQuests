package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;

/**
 * What one holder, a player, a world or the server, was handed by one assignment for one of the
 * quests it lists: how many times, when last, and on which occasion. Kept apart from the quests,
 * so it still answers for a quest that left no trace.
 *
 * <p>Never changed once built, like {@link QuestCompletions}: a hand-out builds the next one.
 */
public final class AssignmentRecord {

    public static final BuilderCodec<AssignmentRecord> CODEC = BuilderCodec.builder(AssignmentRecord.class, AssignmentRecord::new)
                                                                           .append(new KeyedCodec<>("Count", Codec.INTEGER), (record, count) -> record.count = count, record -> Integer.valueOf(record.count))
                                                                           .add()
                                                                           .append(new KeyedCodec<>("LastAt", Codec.LONG), (record, millis) -> record.lastAt = millis, record -> Long.valueOf(record.lastAt))
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Occasion", Codec.STRING), (record, occasion) -> record.occasion = occasion, record -> record.occasion)
                                                                           .add()
                                                                           .build();

    private int count;
    private long lastAt;
    private String occasion;

    private AssignmentRecord() {}

    public AssignmentRecord(int count, long lastAt, @Nonnull String occasion) {
        this.count = count;
        this.lastAt = lastAt;
        this.occasion = occasion;
    }

    /**
     * @return the record once more is handed out on that occasion, starting from nothing.
     */
    @Nonnull
    public static AssignmentRecord next(@Nullable AssignmentRecord previous, @Nonnull String occasion, @Nonnull Instant at) {
        return new AssignmentRecord(previous == null ? 1 : previous.count + 1, at.toEpochMilli(), occasion);
    }

    /**
     * @return how many times the quest was handed out to this holder.
     */
    public int getCount() {
        return count;
    }

    /**
     * @return when it was handed out last.
     */
    @Nonnull
    public Instant getLastAt() {
        return Instant.ofEpochMilli(lastAt);
    }

    /**
     * @return the occasion it was handed out on last.
     */
    @Nonnull
    public String getOccasion() {
        return occasion;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AssignmentRecord record && count == record.count && lastAt == record.lastAt && Objects.equals(occasion, record.occasion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, lastAt, occasion);
    }
}
