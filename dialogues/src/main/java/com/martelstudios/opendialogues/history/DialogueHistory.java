package com.martelstudios.opendialogues.history;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * What a player did with each dialogue: how often it opened and how often each answer was picked,
 * kept apart for each NPC character it was had with. Conditions read it to tell a first meeting
 * from the next ones.
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
                                                                    .build();

        private int opened;
        private final Map<String, Integer> answers = new HashMap<>();

        private Entry() {}

        private Entry(@Nonnull Entry other) {
            opened = other.opened;
            answers.putAll(other.answers);
        }
    }

    /**
     * Key of the dialogues had without an NPC character, opened by a command or an item.
     */
    private static final String NO_CHARACTER = "";

    private static final MapCodec<Entry, Map<String, Entry>> DIALOGUES_CODEC = new MapCodec<>(Entry.CODEC, HashMap<String, Entry>::new);

    public static final BuilderCodec<DialogueHistory> CODEC = BuilderCodec.builder(DialogueHistory.class, DialogueHistory::new)
                                                                          .append(new KeyedCodec<>("Characters", new MapCodec<>(DIALOGUES_CODEC, HashMap<String, Map<String, Entry>>::new)), (history, characters) -> characters.forEach((character, dialogues) -> history.dialoguesOf(character).putAll(dialogues)), history -> history.characters)
                                                                          .add()
                                                                          // Histories saved before characters were counted, read as had without one.
                                                                          .append(new KeyedCodec<>("Dialogues", DIALOGUES_CODEC), (history, dialogues) -> history.dialoguesOf(NO_CHARACTER).putAll(dialogues), history -> null)
                                                                          .add()
                                                                          .build();

    private final Map<String, Map<String, Entry>> characters = new HashMap<>();

    public DialogueHistory() {}

    private DialogueHistory(@Nonnull DialogueHistory other) {
        other.characters.forEach((character, dialogues) -> dialogues.forEach((id, entry) -> dialoguesOf(character).put(id, new Entry(entry))));
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
        entry(characterId, dialogueId).opened++;
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
        entry(characterId, dialogueId).answers.merge(answerKey, 1, Integer::sum);
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
        for (Map<String, Entry> dialogues : characters.values()) cleared |= dialogues.remove(dialogueId) != null;
        characters.values().removeIf(Map::isEmpty);
        return cleared;
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
        int total = 0;
        for (Map.Entry<String, Map<String, Entry>> character : characters.entrySet()) {
            if (characterId != null && !characterId.equals(character.getKey())) continue;
            for (Map.Entry<String, Entry> dialogue : character.getValue().entrySet()) {
                if (dialogueId != null && !dialogueId.equals(dialogue.getKey())) continue;
                Entry entry = dialogue.getValue();
                total += answerKey == null ? entry.opened : entry.answers.getOrDefault(answerKey, 0);
            }
        }
        return total;
    }

    @Nonnull
    private Entry entry(@Nullable String characterId, @Nonnull String dialogueId) {
        return dialoguesOf(characterId == null ? NO_CHARACTER : characterId).computeIfAbsent(dialogueId, id -> new Entry());
    }

    @Nonnull
    private Map<String, Entry> dialoguesOf(@Nonnull String characterKey) {
        return characters.computeIfAbsent(characterKey, key -> new HashMap<>());
    }
}
