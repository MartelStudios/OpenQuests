package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * How the quests each NPC handed a player ended, kept on the player because ended quests are
 * archived and too costly to read back on every condition check.
 */
public class GiverQuestRecord implements Component<EntityStore> {

    private static final MapCodec<QuestCompletions, Map<String, QuestCompletions>> ASSETS_CODEC = new MapCodec<>(QuestCompletions.CODEC, HashMap<String, QuestCompletions>::new);

    private static final MapCodec<Map<String, QuestCompletions>, Map<String, Map<String, QuestCompletions>>> NPCS_CODEC = new MapCodec<>(ASSETS_CODEC, HashMap<String, Map<String, QuestCompletions>>::new);

    public static final BuilderCodec<GiverQuestRecord> CODEC = BuilderCodec.builder(GiverQuestRecord.class, GiverQuestRecord::new)
                                                                           .append(new KeyedCodec<>("Givers", new MapCodec<>(NPCS_CODEC, HashMap<String, Map<String, Map<String, QuestCompletions>>>::new)), (record, givers) -> givers.forEach((character, npcs) -> npcs.forEach((npc, assets) -> assets.forEach((asset, completions) -> record.assetsOf(character, npc).put(asset, completions)))), record -> record.givers)
                                                                           .documentation("Quest outcomes by giver character ID, then giver NPC UUID, then quest asset ID, initially empty.")
                                                                           .add()
                                                                           .build();

    /**
     * Character ID, then NPC UUID, then quest asset ID. Counts are immutable, so a copy shares them.
     */
    private final Map<String, Map<String, Map<String, QuestCompletions>>> givers = new HashMap<>();

    public GiverQuestRecord() {}

    private GiverQuestRecord(@Nonnull GiverQuestRecord other) {
        other.givers.forEach((character, npcs) -> npcs.forEach((npc, assets) -> assetsOf(character, npc).putAll(assets)));
    }

    @Override
    public Component<EntityStore> clone() {
        return new GiverQuestRecord(this);
    }

    /**
     * Keeps when a quest from that giver started, unless a later start is already kept.
     */
    public void started(@Nonnull Giver giver, @Nonnull String assetId, @Nonnull Instant at) {
        assetsOf(giver.character(), giver.npc().toString()).compute(assetId, (id, current) -> {
            QuestCompletions now = current == null ? QuestCompletions.NONE : current;
            return QuestCompletions.of(now.count(QuestState.SUCCESSFUL), now.count(QuestState.FAILED), now.count(QuestState.ABANDONED), latest(now.getLastStartedAt(), at), now.getLastCompletedAt());
        });
    }

    /**
     * Counts one more quest from that giver ended that way.
     */
    public void ended(@Nonnull Giver giver, @Nonnull String assetId, @Nonnull QuestState state, @Nonnull Instant at) {
        assetsOf(giver.character(), giver.npc().toString()).compute(assetId, (id, current) -> (current == null ? QuestCompletions.NONE : current).record(state, null, at));
    }

    /**
     * @param npc the one NPC to read, {@code null} to sum every NPC of the character.
     * @return how the quests from that asset handed by that giver ended, {@link QuestCompletions#NONE} when none did.
     */
    @Nonnull
    public QuestCompletions completions(@Nonnull String character, @Nullable UUID npc, @Nonnull String assetId) {
        Map<String, Map<String, QuestCompletions>> npcs = givers.getOrDefault(character, Map.of());
        if (npc != null) return npcs.getOrDefault(npc.toString(), Map.of()).getOrDefault(assetId, QuestCompletions.NONE);

        QuestCompletions sum = QuestCompletions.NONE;
        for (Map<String, QuestCompletions> assets : npcs.values()) {
            QuestCompletions one = assets.get(assetId);
            if (one == null) continue;
            sum = sum == QuestCompletions.NONE ? one : QuestCompletions.of(sum.count(QuestState.SUCCESSFUL) + one.count(QuestState.SUCCESSFUL), sum.count(QuestState.FAILED) + one.count(QuestState.FAILED), sum.count(QuestState.ABANDONED) + one.count(QuestState.ABANDONED), latest(sum.getLastStartedAt(), one.getLastStartedAt()), latest(sum.getLastCompletedAt(), one.getLastCompletedAt()));
        }
        return sum;
    }

    /**
     * Drops everything an NPC handed out, under whichever character.
     *
     * @return whether anything was dropped.
     */
    public boolean forgetNpc(@Nonnull UUID npc) {
        boolean dropped = false;
        for (Map<String, Map<String, QuestCompletions>> npcs : givers.values()) {
            dropped |= npcs.remove(npc.toString()) != null;
        }
        givers.values().removeIf(Map::isEmpty);
        return dropped;
    }

    /**
     * Drops everything handed out by the NPCs the predicate names, under whichever character.
     *
     * @return whether anything was dropped.
     */
    public boolean forgetNpcs(@Nonnull Predicate<UUID> released) {
        boolean dropped = false;
        for (Map<String, Map<String, QuestCompletions>> npcs : givers.values()) {
            dropped |= npcs.keySet().removeIf(key -> {
                try {
                    return released.test(UUID.fromString(key));
                } catch (IllegalArgumentException malformed) {
                    return false;
                }
            });
        }
        givers.values().removeIf(Map::isEmpty);
        return dropped;
    }

    @Nonnull
    private Map<String, QuestCompletions> assetsOf(@Nonnull String character, @Nonnull String npc) {
        return givers.computeIfAbsent(character, key -> new HashMap<>()).computeIfAbsent(npc, key -> new HashMap<>());
    }

    @Nullable
    private static Instant latest(@Nullable Instant current, @Nullable Instant candidate) {
        if (current == null) return candidate;
        if (candidate == null) return current;

        return candidate.isAfter(current) ? candidate : current;
    }
}
