package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What one player carries, apart from the progressions themselves. Read whole on connection and
 * written whole on departure, so a second server picks them up where the first left off.
 */
public class PlayerQuestRecord {

    public static final BuilderCodec<PlayerQuestRecord> CODEC = BuilderCodec.builder(PlayerQuestRecord.class, PlayerQuestRecord::new)
                                                                            .append(new KeyedCodec<>("Quests", new SetCodec<>(Codec.UUID_STRING, HashSet<UUID>::new, false)), (record, ids) -> record.questIds.addAll(ids), record -> record.questIds)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("StartedOnConnection", new SetCodec<>(Codec.STRING, HashSet<String>::new, false)), (record, ids) -> record.startedOnConnection.addAll(ids), record -> record.startedOnConnection)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("PendingRewards", new SetCodec<>(PendingRewards.CODEC, HashSet<PendingRewards>::new, false)), (record, owed) -> record.pendingRewards.addAll(owed), record -> record.pendingRewards)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Completions", new MapCodec<>(QuestCompletions.CODEC, HashMap<String, QuestCompletions>::new)), (record, completions) -> record.completions.putAll(completions), record -> record.completions)
                                                                            .add()
                                                                            .build();

    private final Set<UUID> questIds = new HashSet<>();

    private final Set<String> startedOnConnection = new HashSet<>();

    private final Set<PendingRewards> pendingRewards = new HashSet<>();

    private final Map<String, QuestCompletions> completions = new HashMap<>();

    public PlayerQuestRecord() {}

    public PlayerQuestRecord(@Nonnull Set<UUID> questIds, @Nonnull Set<String> startedOnConnection, @Nonnull Set<PendingRewards> pendingRewards, @Nonnull Map<String, QuestCompletions> completions) {
        this.questIds.addAll(questIds);
        this.startedOnConnection.addAll(startedOnConnection);
        this.pendingRewards.addAll(pendingRewards);
        this.completions.putAll(completions);
    }

    /**
     * @return the ids of every quest this player takes part in: where to look, not what they are.
     */
    @Nonnull
    public Set<UUID> getQuestIds() {
        return questIds;
    }

    /**
     * @return the asset ids already handed to this player by {@code StartOnConnection}. Only these
     * are kept, not the quests made from them, so a catalogue offered to everyone costs one string
     * per quest actually taken.
     */
    @Nonnull
    public Set<String> getStartedOnConnection() {
        return startedOnConnection;
    }

    /**
     * @return every debt still standing, one entry per completion.
     */
    @Nonnull
    public Set<PendingRewards> getPendingRewards() {
        return pendingRewards;
    }

    /**
     * @return what the player has done with each asset, by asset id. Only the assets they ended a
     * quest from are listed.
     */
    @Nonnull
    public Map<String, QuestCompletions> getCompletions() {
        return completions;
    }

    /**
     * Counts one more quest from that asset ended that way.
     */
    public void recordCompletion(@Nonnull String assetId, @Nonnull QuestState outcome, @Nullable Instant startedAt, @Nullable Instant completedAt) {
        completions.compute(assetId, (id, current) -> (current == null ? QuestCompletions.NONE : current).record(outcome, startedAt, completedAt));
    }

    /**
     * @return {@code true} for a player who has never held a quest, which is the one case a
     * backend may answer without having anything written down.
     */
    public boolean isEmpty() {
        return questIds.isEmpty() && startedOnConnection.isEmpty() && pendingRewards.isEmpty() && completions.isEmpty();
    }
}
