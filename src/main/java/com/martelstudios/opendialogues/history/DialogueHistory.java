package com.martelstudios.opendialogues.history;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * What a player did with each dialogue: how often it opened and how often each answer was picked.
 * Conditions read it to tell a first meeting from the next ones.
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

    public static final BuilderCodec<DialogueHistory> CODEC = BuilderCodec.builder(DialogueHistory.class, DialogueHistory::new)
                                                                          .append(new KeyedCodec<>("Dialogues", new MapCodec<>(Entry.CODEC, HashMap<String, Entry>::new)), (history, dialogues) -> history.dialogues.putAll(dialogues), history -> history.dialogues)
                                                                          .add()
                                                                          .build();

    private final Map<String, Entry> dialogues = new HashMap<>();

    public DialogueHistory() {}

    private DialogueHistory(@Nonnull DialogueHistory other) {
        other.dialogues.forEach((id, entry) -> dialogues.put(id, new Entry(entry)));
    }

    @Override
    public Component<EntityStore> clone() {
        return new DialogueHistory(this);
    }

    /**
     * Counts one more opening of a dialogue.
     */
    public void recordOpened(@Nonnull String dialogueId) {
        dialogues.computeIfAbsent(dialogueId, id -> new Entry()).opened++;
    }

    /**
     * Counts one more pick of an answer, under its key in the dialogue.
     */
    public void recordAnswer(@Nonnull String dialogueId, @Nonnull String answerKey) {
        dialogues.computeIfAbsent(dialogueId, id -> new Entry()).answers.merge(answerKey, 1, Integer::sum);
    }

    /**
     * @return how many times the dialogue opened, 0 for one never seen.
     */
    public int opened(@Nonnull String dialogueId) {
        Entry entry = dialogues.get(dialogueId);
        return entry == null ? 0 : entry.opened;
    }

    /**
     * @return how many times that answer was picked, 0 for one never picked.
     */
    public int answered(@Nonnull String dialogueId, @Nonnull String answerKey) {
        Entry entry = dialogues.get(dialogueId);
        return entry == null ? 0 : entry.answers.getOrDefault(answerKey, 0);
    }
}
