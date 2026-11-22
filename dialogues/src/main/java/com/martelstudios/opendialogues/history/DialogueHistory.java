package com.martelstudios.opendialogues.history;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Consumer;

/**
 * What a player did with each dialogue: how often it opened and how often each answer was picked,
 * kept apart for each NPC character, and each NPC of it, it was had with. Conditions read it to
 * tell a first meeting from the next ones.
 */
public class DialogueHistory implements Component<EntityStore> {

    /**
     * Counts for one dialogue.
     */
    public static class Entry {

        public static final BuilderCodec<Entry> CODEC = BuilderCodec.builder(Entry.class, Entry::new)
                                                                    .append(new KeyedCodec<>("Opened", Codec.INTEGER), (entry, opened) -> entry.opened = opened, entry -> entry.opened)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("Answers", new MapCodec<>(Codec.INTEGER, HashMap<String, Integer>::new)), (entry, answers) -> entry.answers.putAll(answers), entry -> entry.answers)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("LastOpenedAt", Codec.LONG), (entry, time) -> entry.lastOpenedAt = time, entry -> entry.lastOpenedAt)
                                                                    .documentation("Epoch milliseconds of the latest opening, unset before any timestamped opening.")
                                                                    .add()
                                                                    .append(new KeyedCodec<>("LastAnsweredAt", new MapCodec<>(Codec.LONG, HashMap<String, Long>::new)), (entry, answers) -> entry.lastAnsweredAt.putAll(answers), entry -> entry.lastAnsweredAt)
                                                                    .documentation("Epoch milliseconds of the latest pick by answer ID, initially empty.")
                                                                    .add()
                                                                    .build();

        private int opened;
        private final Map<String, Integer> answers = new HashMap<>();
        private Long lastOpenedAt;
        private final Map<String, Long> lastAnsweredAt = new HashMap<>();

        private Entry() {}

        private Entry(@Nonnull Entry other) {
            opened = other.opened;
            answers.putAll(other.answers);
            lastOpenedAt = other.lastOpenedAt;
            lastAnsweredAt.putAll(other.lastAnsweredAt);
        }
    }

    /**
     * Key of the dialogues had without an NPC character, opened by a command or an item.
     */
    private static final String NO_CHARACTER = "";

    /**
     * Key of the dialogues had without a known NPC, including those saved before NPCs were counted.
     */
    private static final String NO_NPC = "";

    private static final MapCodec<Entry, Map<String, Entry>> DIALOGUES_CODEC = new MapCodec<>(Entry.CODEC, HashMap<String, Entry>::new);

    private static final MapCodec<Map<String, Entry>, Map<String, Map<String, Entry>>> KEYED_DIALOGUES_CODEC = new MapCodec<>(DIALOGUES_CODEC, HashMap<String, Map<String, Entry>>::new);

    public static final BuilderCodec<DialogueHistory> CODEC = BuilderCodec.builder(DialogueHistory.class, DialogueHistory::new)
                                                                          .append(new KeyedCodec<>("Characters", KEYED_DIALOGUES_CODEC), (history, characters) -> characters.forEach((character, dialogues) -> history.dialoguesOf(character, NO_NPC).putAll(dialogues)), DialogueHistory::withoutNpc)
                                                                          .documentation("Counts by character ID, then dialogue ID, for dialogues had without a known NPC. Empty key for no character.")
                                                                          .add()
                                                                          .append(new KeyedCodec<>("Npcs", new MapCodec<>(KEYED_DIALOGUES_CODEC, HashMap<String, Map<String, Map<String, Entry>>>::new)), (history, characters) -> characters.forEach((character, npcs) -> npcs.forEach((npc, dialogues) -> history.dialoguesOf(character, npc).putAll(dialogues))), DialogueHistory::withNpc)
                                                                          .documentation("Counts by character ID, then NPC UUID, then dialogue ID, initially empty.")
                                                                          .add()
                                                                          // Histories saved before characters were counted, read as had without one.
                                                                          .append(new KeyedCodec<>("Dialogues", DIALOGUES_CODEC), (history, dialogues) -> history.dialoguesOf(NO_CHARACTER, NO_NPC).putAll(dialogues), history -> null)
                                                                          .add()
                                                                          .build();

    /**
     * Character key, then NPC key, then dialogue ID.
     */
    private final Map<String, Map<String, Map<String, Entry>>> characters = new HashMap<>();

    public DialogueHistory() {}

    private DialogueHistory(@Nonnull DialogueHistory other) {
        other.characters.forEach((character, npcs) -> npcs.forEach((npc, dialogues) -> dialogues.forEach((id, entry) -> dialoguesOf(character, npc).put(id, new Entry(entry)))));
    }

    @Override
    public Component<EntityStore> clone() {
        return new DialogueHistory(this);
    }

    /**
     * Counts one more opening of a dialogue had without an NPC character.
     */
    public void recordOpened(@Nonnull String dialogueId) {
        recordOpened(null, dialogueId);
    }

    /**
     * Counts one more opening of a dialogue, under the character it was had with.
     *
     * @param characterId {@code null} for a dialogue had without one.
     */
    public void recordOpened(@Nullable String characterId, @Nonnull String dialogueId) {
        recordOpened(characterId, dialogueId, Instant.now());
    }

    /**
     * Counts one more opening and keeps its time, unless a later one is already recorded.
     *
     * @param characterId {@code null} for a dialogue had without one.
     */
    public void recordOpened(@Nullable String characterId, @Nonnull String dialogueId, @Nonnull Instant occurredAt) {
        recordOpened(characterId, null, dialogueId, occurredAt);
    }

    /**
     * Counts one more opening under the character and NPC it was had with, keeping its time unless a later one is recorded.
     *
     * @param npcId {@code null} for a dialogue had without a known NPC.
     */
    public void recordOpened(@Nullable String characterId, @Nullable UUID npcId, @Nonnull String dialogueId, @Nonnull Instant occurredAt) {
        Entry entry = entry(characterId, npcId, dialogueId);
        long millis = occurredAt.toEpochMilli();
        entry.opened++;
        if (entry.lastOpenedAt == null || millis > entry.lastOpenedAt) entry.lastOpenedAt = millis;
    }

    /**
     * Counts one more pick of an answer, under its key in a dialogue had without an NPC character.
     */
    public void recordAnswer(@Nonnull String dialogueId, @Nonnull String answerKey) {
        recordAnswer(null, dialogueId, answerKey);
    }

    /**
     * Counts one more pick of an answer, under its key in the dialogue and the character it was had with.
     *
     * @param characterId {@code null} for a dialogue had without one.
     */
    public void recordAnswer(@Nullable String characterId, @Nonnull String dialogueId, @Nonnull String answerKey) {
        recordAnswer(characterId, dialogueId, answerKey, Instant.now());
    }

    /**
     * Counts one more pick of an answer and keeps its time, unless a later one is already recorded.
     *
     * @param characterId {@code null} for a dialogue had without one.
     */
    public void recordAnswer(@Nullable String characterId, @Nonnull String dialogueId, @Nonnull String answerKey, @Nonnull Instant occurredAt) {
        recordAnswer(characterId, null, dialogueId, answerKey, occurredAt);
    }

    /**
     * Counts one more pick of an answer under the character and NPC it was had with, keeping its time unless a later one is recorded.
     *
     * @param npcId {@code null} for a dialogue had without a known NPC.
     */
    public void recordAnswer(@Nullable String characterId, @Nullable UUID npcId, @Nonnull String dialogueId, @Nonnull String answerKey, @Nonnull Instant occurredAt) {
        Entry entry = entry(characterId, npcId, dialogueId);
        entry.answers.merge(answerKey, 1, Integer::sum);
        entry.lastAnsweredAt.merge(answerKey, occurredAt.toEpochMilli(), Math::max);
    }

    /**
     * Latest timestamp for the dialogue or answer. A null character matches every character,
     * including dialogues without one. A null answer matches openings.
     */
    @Nullable
    public Instant lastOccurrence(@Nullable String characterId, @Nonnull String dialogueId, @Nullable String answerKey) {
        return lastOccurrence(characterId, null, dialogueId, answerKey);
    }

    /**
     * Latest timestamp for the dialogue or answer, a {@code null} character or NPC matching any.
     */
    @Nullable
    public Instant lastOccurrence(@Nullable String characterId, @Nullable UUID npcId, @Nonnull String dialogueId, @Nullable String answerKey) {
        long[] latest = {Long.MIN_VALUE};
        forEachEntry(characterId, npcId, dialogueId, entry -> {
            Long time = answerKey == null ? entry.lastOpenedAt : entry.lastAnsweredAt.get(answerKey);
            if (time != null && time > latest[0]) latest[0] = time;
        });
        return latest[0] == Long.MIN_VALUE ? null : Instant.ofEpochMilli(latest[0]);
    }

    /**
     * Forgets everything the player did, as if they never met a dialogue.
     *
     * @return whether anything was recorded.
     */
    public boolean clear() {
        if (characters.isEmpty()) return false;
        characters.clear();
        return true;
    }

    /**
     * Forgets one dialogue's openings and answers with every character, the others kept.
     *
     * @return whether that dialogue had anything recorded.
     */
    public boolean clear(@Nonnull String dialogueId) {
        boolean cleared = false;
        for (Map<String, Map<String, Entry>> npcs : characters.values()) {
            for (Map<String, Entry> dialogues : npcs.values()) cleared |= dialogues.remove(dialogueId) != null;
            npcs.values().removeIf(Map::isEmpty);
        }
        characters.values().removeIf(Map::isEmpty);
        return cleared;
    }

    /**
     * Forgets what the player did with one NPC, under every character.
     *
     * @return whether that NPC had anything recorded.
     */
    public boolean forgetNpc(@Nonnull UUID npcId) {
        String key = npcId.toString();
        boolean forgotten = false;
        for (Map<String, Map<String, Entry>> npcs : characters.values()) forgotten |= npcs.remove(key) != null;
        characters.values().removeIf(Map::isEmpty);
        return forgotten;
    }

    /**
     * Forgets what the player did with every NPC the predicate names, under every character.
     *
     * @return whether anything was forgotten.
     */
    public boolean forgetNpcs(@Nonnull Predicate<UUID> released) {
        boolean forgotten = false;
        for (Map<String, Map<String, Entry>> npcs : characters.values()) forgotten |= npcs.keySet().removeIf(key -> isNpc(key, released));
        characters.values().removeIf(Map::isEmpty);
        return forgotten;
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

    /**
     * @return how many times the dialogue opened, with any character, 0 for one never seen.
     */
    public int opened(@Nonnull String dialogueId) {
        return count(null, dialogueId, null);
    }

    /**
     * @return how many times that answer was picked, with any character, 0 for one never picked.
     */
    public int answered(@Nonnull String dialogueId, @Nonnull String answerKey) {
        return count(null, dialogueId, answerKey);
    }

    /**
     * Sums what matches every filter given, {@code null} matching anything.
     *
     * @param answerKey {@code null} counts openings, otherwise the picks of that answer.
     */
    public int count(@Nullable String characterId, @Nullable String dialogueId, @Nullable String answerKey) {
        return count(characterId, null, dialogueId, answerKey);
    }

    /**
     * Sums what matches every filter given, {@code null} matching anything.
     *
     * @param answerKey {@code null} counts openings, otherwise the picks of that answer.
     */
    public int count(@Nullable String characterId, @Nullable UUID npcId, @Nullable String dialogueId, @Nullable String answerKey) {
        int[] total = {0};
        forEachEntry(characterId, npcId, dialogueId, entry -> total[0] += answerKey == null ? entry.opened : entry.answers.getOrDefault(answerKey, 0));
        return total[0];
    }

    private void forEachEntry(@Nullable String characterId, @Nullable UUID npcId, @Nullable String dialogueId, @Nonnull Consumer<Entry> action) {
        String npcKey = npcId == null ? null : npcId.toString();
        for (Map.Entry<String, Map<String, Map<String, Entry>>> character : characters.entrySet()) {
            if (characterId != null && !characterId.equals(character.getKey())) continue;
            for (Map.Entry<String, Map<String, Entry>> npc : character.getValue().entrySet()) {
                if (npcKey != null && !npcKey.equals(npc.getKey())) continue;
                for (Map.Entry<String, Entry> dialogue : npc.getValue().entrySet()) {
                    if (dialogueId == null || dialogueId.equals(dialogue.getKey())) action.accept(dialogue.getValue());
                }
            }
        }
    }

    @Nonnull
    private Entry entry(@Nullable String characterId, @Nullable UUID npcId, @Nonnull String dialogueId) {
        return dialoguesOf(characterId == null ? NO_CHARACTER : characterId, npcId == null ? NO_NPC : npcId.toString()).computeIfAbsent(dialogueId, id -> new Entry());
    }

    @Nonnull
    private Map<String, Entry> dialoguesOf(@Nonnull String characterKey, @Nonnull String npcKey) {
        return characters.computeIfAbsent(characterKey, key -> new HashMap<>()).computeIfAbsent(npcKey, key -> new HashMap<>());
    }

    @Nonnull
    private Map<String, Map<String, Entry>> withoutNpc() {
        Map<String, Map<String, Entry>> saved = new HashMap<>();
        characters.forEach((character, npcs) -> {
            Map<String, Entry> dialogues = npcs.get(NO_NPC);
            if (dialogues != null) saved.put(character, dialogues);
        });
        return saved;
    }

    @Nonnull
    private Map<String, Map<String, Map<String, Entry>>> withNpc() {
        Map<String, Map<String, Map<String, Entry>>> saved = new HashMap<>();
        characters.forEach((character, npcs) -> npcs.forEach((npc, dialogues) -> {
            if (!npc.equals(NO_NPC)) saved.computeIfAbsent(character, key -> new HashMap<>()).put(npc, dialogues);
        }));
        return saved;
    }
}
