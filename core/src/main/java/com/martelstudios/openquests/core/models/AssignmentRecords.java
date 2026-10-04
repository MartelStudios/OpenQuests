package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The assignment records of one holder, by assignment id and then by the id of the quest it
 * handed out, since one assignment can list several and constraints may refuse one alone.
 */
public final class AssignmentRecords {

    /**
     * Written as the nested map itself, {@code { "<assignment>": { "<quest>": { … } } }}.
     */
    public static final Codec<Map<String, Map<String, AssignmentRecord>>> MAP_CODEC = new MapCodec<>(new MapCodec<>(AssignmentRecord.CODEC, HashMap<String, AssignmentRecord>::new), HashMap<String, Map<String, AssignmentRecord>>::new);

    private final Map<String, Map<String, AssignmentRecord>> records = new ConcurrentHashMap<>();

    public AssignmentRecords() {}

    public AssignmentRecords(@Nonnull Map<String, Map<String, AssignmentRecord>> records) {
        putAll(records);
    }

    /**
     * @return what that assignment handed out of that quest, {@code null} if nothing yet.
     */
    @Nullable
    public AssignmentRecord get(@Nonnull String assignmentId, @Nonnull String questAssetId) {
        Map<String, AssignmentRecord> byQuest = records.get(assignmentId);
        return byQuest == null ? null : byQuest.get(questAssetId);
    }

    /**
     * Writes down a hand-out, replacing what that assignment had handed out of that quest.
     */
    public void put(@Nonnull String assignmentId, @Nonnull String questAssetId, @Nonnull AssignmentRecord record) {
        records.computeIfAbsent(assignmentId, id -> new ConcurrentHashMap<>()).put(questAssetId, record);
    }

    /**
     * Takes these instead of what was there, which is what reading a holder back amounts to.
     */
    public void replaceAll(@Nonnull Map<String, Map<String, AssignmentRecord>> records) {
        this.records.clear();
        putAll(records);
    }

    /**
     * @return a copy fit for writing out, detached from what changes next.
     */
    @Nonnull
    public Map<String, Map<String, AssignmentRecord>> snapshot() {
        Map<String, Map<String, AssignmentRecord>> copy = new HashMap<>();
        records.forEach((assignmentId, byQuest) -> copy.put(assignmentId, new HashMap<>(byQuest)));
        return copy;
    }

    /**
     * @return whether nothing was ever handed out to this holder.
     */
    public boolean isEmpty() {
        return records.isEmpty();
    }

    private void putAll(@Nonnull Map<String, Map<String, AssignmentRecord>> records) {
        records.forEach((assignmentId, byQuest) -> this.records.put(assignmentId, new ConcurrentHashMap<>(byQuest)));
    }
}
