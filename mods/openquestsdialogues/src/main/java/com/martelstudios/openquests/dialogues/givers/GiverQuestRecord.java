package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.runtime.ReleasedNpcs;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
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
                                                                           .documentation("Quest outcomes by giver character ID, then giver NPC UUID (empty key for NPCs since released), then quest asset ID, initially empty.")
                                                                           .add()
                                                                           .documentation("Stores how the quests each NPC handed a player ended.")
                                                                           .build();

    /**
     * Character ID, then NPC UUID, then quest asset ID. Counts are immutable, so a copy shares them.
     */
    private final Map<String, Map<String, Map<String, QuestCompletions>>> givers = new HashMap<>();

    /** Key of the outcomes from NPCs since released, counted for their character alone. */
    private static final String NO_NPC = "";

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
    public void started(@Nonnull CharacterGiver giver, @Nonnull String assetId, @Nonnull Instant at) {
        assetsOf(giver.getCharacter(), giver.getNpc().toString()).compute(assetId, (id, current) -> {
            QuestCompletions now = current == null ? QuestCompletions.NONE : current;
            return QuestCompletions.of(now.count(QuestState.SUCCESSFUL), now.count(QuestState.FAILED), now.count(QuestState.ABANDONED), latest(now.getLastStartedAt(), at), now.getLastCompletedAt());
        });
    }

    /**
     * Counts one more quest from that giver ended that way.
     */
    public void ended(@Nonnull CharacterGiver giver, @Nonnull String assetId, @Nonnull QuestState state, @Nonnull Instant at) {
        assetsOf(giver.getCharacter(), giver.getNpc().toString()).compute(assetId, (id, current) -> (current == null ? QuestCompletions.NONE : current).record(state, null, at));
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
            sum = sum == QuestCompletions.NONE ? one : sum(sum, one);
        }
        return sum;
    }

    /**
     * Forgets one NPC, under whichever character: its outcomes count for its character only, as
     * handed by no known NPC, so conditions on the character keep their counts.
     *
     * @return whether that NPC had anything recorded.
     */
    public boolean forgetNpc(@Nonnull UUID npc) {
        return forgetNpcs(id -> id.equals(npc) ? Instant.MAX : null);
    }

    /**
     * Forgets, as {@link #forgetNpc} does, each NPC released after every quest it handed out under a
     * character: one handing quests out again since, carrying that character anew, is kept.
     *
     * @param releasedAt when each NPC last let its character go, {@code null} for one not released.
     * @return whether anything was forgotten.
     */
    public boolean forgetNpcs(@Nonnull Function<UUID, Instant> releasedAt) {
        boolean forgotten = false;
        for (Map<String, Map<String, QuestCompletions>> npcs : givers.values()) {
            for (String key : List.copyOf(npcs.keySet())) {
                if (!isNpc(key, npc -> ReleasedNpcs.releasedSince(releasedAt.apply(npc), latest(npcs.get(key))))) continue;
                Map<String, QuestCompletions> unknown = npcs.computeIfAbsent(NO_NPC, none -> new HashMap<>());
                npcs.remove(key).forEach((assetId, completions) -> unknown.merge(assetId, completions, GiverQuestRecord::sum));
                forgotten = true;
            }
        }
        return forgotten;
    }

    /**
     * @return the latest start or end among those outcomes, {@code null} when none is dated.
     */
    @Nullable
    private static Instant latest(@Nonnull Map<String, QuestCompletions> outcomes) {
        Instant latest = null;
        for (QuestCompletions completions : outcomes.values()) {
            for (Instant at : new Instant[] {completions.getLastStartedAt(), completions.getLastCompletedAt()}) {
                if (at != null && (latest == null || at.isAfter(latest))) latest = at;
            }
        }
        return latest;
    }

    /**
     * @return whether the key names an NPC the predicate holds; the no-NPC key and malformed ones never do.
     */
    private static boolean isNpc(@Nonnull String key, @Nonnull Predicate<UUID> released) {
        if (key.equals(NO_NPC)) return false;
        try {
            return released.test(UUID.fromString(key));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    @Nonnull
    private static QuestCompletions sum(@Nonnull QuestCompletions first, @Nonnull QuestCompletions second) {
        return QuestCompletions.of(first.count(QuestState.SUCCESSFUL) + second.count(QuestState.SUCCESSFUL), first.count(QuestState.FAILED) + second.count(QuestState.FAILED), first.count(QuestState.ABANDONED) + second.count(QuestState.ABANDONED), latest(first.getLastStartedAt(), second.getLastStartedAt()), latest(first.getLastCompletedAt(), second.getLastCompletedAt()));
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
