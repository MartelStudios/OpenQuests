package com.martelstudios.opendialogues.history;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.dialogues.DialogueResponse;
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * The one writer of {@link DialogueHistory}: counts what players open and answer as the events
 * come, so the service opening dialogues knows nothing of storage.
 */
public class DialogueHistoryRecorder {

    @Nonnull
    private final ComponentType<EntityStore, DialogueHistory> type;

    public DialogueHistoryRecorder(@Nonnull ComponentType<EntityStore, DialogueHistory> type) {
        this.type = type;
    }

    /**
     * Counts the opening for the dialogue's player.
     */
    public void onOpened(@Nonnull DialogueEvent.Opened event) {
        String dialogueId = event.getSession().dialogueId();
        write(event.getPlayer(), history -> history.recordOpened(dialogueId));
    }

    /**
     * Counts the answer picked, under its key in the dialogue.
     */
    public void onAnswered(@Nonnull DialogueEvent.Answered event) {
        DialogueSession session = event.getSession();
        DialogueResponse response = session.responses().get(event.getPosition() - 1);
        String answerKey = session.dialogue().answerKey(response);
        write(event.getPlayer(), history -> history.recordAnswer(session.dialogueId(), answerKey));
    }

    /**
     * @return the player's history, created when missing, or {@code null} when their entity cannot
     * be read from this thread.
     */
    @Nullable
    public DialogueHistory history(@Nonnull PlayerRef player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return null;

        Store<EntityStore> store = ref.getStore();
        return store.isInThread() ? store.ensureAndGetComponent(ref, type) : null;
    }

    /**
     * Writes in place on the world thread, which events reach before an answer's actions run, so a
     * router opened by that answer already reads it. From elsewhere the write hops there and may land late.
     */
    private void write(@Nonnull PlayerRef player, @Nonnull Consumer<DialogueHistory> change) {
        DialogueHistory history = history(player);
        if (history != null) {
            change.accept(history);
            return;
        }

        World world = player.getWorldUuid() == null ? null : Universe.get().getWorld(player.getWorldUuid());
        if (world == null) return;
        world.execute(() -> {
            DialogueHistory later = history(player);
            if (later != null) change.accept(later);
        });
    }
}
