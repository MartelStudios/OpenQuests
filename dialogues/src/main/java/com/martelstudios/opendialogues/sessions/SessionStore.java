package com.martelstudios.opendialogues.sessions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dialogues each player has open, by dialogue id, in opening order. Several can be open at
 * once, so an answer names its dialogue unless only one is open.
 *
 * @param <S> what is kept per open dialogue.
 */
public class SessionStore<S> {

    private final Map<UUID, LinkedHashMap<String, S>> sessions = new LinkedHashMap<>();

    /**
     * Keeps a dialogue as open, replacing the same dialogue opened earlier.
     */
    public synchronized void put(@Nonnull UUID player, @Nonnull String dialogueId, @Nonnull S session) {
        LinkedHashMap<String, S> open = sessions.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
        open.remove(dialogueId);
        open.put(dialogueId, session);
    }

    /**
     * @return the session taken out, or {@code null} when that dialogue was not open.
     */
    @Nullable
    public synchronized S remove(@Nonnull UUID player, @Nonnull String dialogueId) {
        Map<String, S> open = sessions.get(player);
        if (open == null) {
            return null;
        }
        S removed = open.remove(dialogueId);
        if (open.isEmpty()) {
            sessions.remove(player);
        }
        return removed;
    }

    /**
     * @return every session of the player, taken out, in opening order.
     */
    @Nonnull
    public synchronized List<S> removeAll(@Nonnull UUID player) {
        Map<String, S> open = sessions.remove(player);
        return open == null ? List.of() : List.copyOf(open.values());
    }

    /**
     * @return the ids of the player's open dialogues, in opening order.
     */
    @Nonnull
    public synchronized List<String> openIds(@Nonnull UUID player) {
        Map<String, S> open = sessions.get(player);
        return open == null ? List.of() : List.copyOf(open.keySet());
    }

    /**
     * Finds the dialogue an answer is meant for: the one named, or the only one open.
     *
     * @param dialogueId the dialogue the player named, or {@code null} for none.
     * @return the session, or {@code null} when the named one is not open, or none was named and
     * zero or several are.
     */
    @Nullable
    public synchronized S resolve(@Nonnull UUID player, @Nullable String dialogueId) {
        Map<String, S> open = sessions.get(player);
        if (open == null) {
            return null;
        }
        if (dialogueId != null) {
            return open.get(dialogueId);
        }
        return open.size() == 1 ? open.values().iterator().next() : null;
    }
}
