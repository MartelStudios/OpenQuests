package com.martelstudios.opendialogues.history;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesResponse;
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

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
        OpenDialoguesResponse response = session.responses().get(event.getPosition() - 1);
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
     * Wipes a player's history, online or not: on their world thread, in their holder, or on disk.
     *
     * @param dialogueId the one dialogue to forget, or {@code null} for all of them.
     * @return completes with whether anything was cleared, {@code false} for a player never stored.
     */
    @Nonnull
    public CompletableFuture<Boolean> reset(@Nonnull UUID playerId, @Nullable String dialogueId) {
        CompletableFuture<Boolean> cleared = new CompletableFuture<>();
        Function<DialogueHistory, Boolean> clear = history -> history != null && (dialogueId == null ? history.clear() : history.clear(dialogueId));
        reset(playerId, clear, cleared, 3);
        return cleared;
    }

    /**
     * Reads without ensuring, so a player without history is not given an empty one. A player
     * leaving the world before the queued write runs is followed a few times at most.
     */
    private void reset(@Nonnull UUID playerId, @Nonnull Function<DialogueHistory, Boolean> clear, @Nonnull CompletableFuture<Boolean> cleared, int handovers) {
        PlayerRef online = Universe.get().getPlayer(playerId);
        if (online != null) {
            Ref<EntityStore> ref = online.getReference();
            if (ref != null) {
                ref.getStore().getExternalData().getWorld().execute(() -> {
                    if (ref.isValid()) cleared.complete(clear.apply(ref.getStore().getComponent(ref, type)));
                    else if (handovers > 0) reset(playerId, clear, cleared, handovers - 1);
                    else cleared.complete(false);
                });
                return;
            }
            Holder<EntityStore> holder = online.getHolder();
            if (holder != null) {
                cleared.complete(clear.apply(holder.getComponent(type)));
                return;
            }
        }

        PlayerStorage storage = Universe.get().getPlayerStorage();
        try {
            // An update on a player never stored would save an empty one.
            if (!storage.getPlayers().contains(playerId)) {
                cleared.complete(false);
                return;
            }
        } catch (IOException e) {
            cleared.completeExceptionally(e);
            return;
        }
        // Reported once saved, so a failed write is not announced as a reset.
        boolean[] result = new boolean[1];
        storage.update(playerId, holder -> result[0] = clear.apply(holder.getComponent(type)))
               .whenComplete((ignored, error) -> {
                   if (error != null) cleared.completeExceptionally(error);
                   else cleared.complete(result[0]);
               });
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
