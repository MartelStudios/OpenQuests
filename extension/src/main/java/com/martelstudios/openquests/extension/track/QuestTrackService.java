package com.martelstudios.openquests.extension.track;

import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.extension.track.events.QuestTrackedEvent;
import com.martelstudios.openquests.extension.track.events.QuestUntrackedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Which quests a player is tracking. The asset says what a quest starts out as through
 * {@code AutoTrack}, the quest itself carries what the player made of it, and everything drawing a
 * quest as tracked asks here rather than reading either.
 *
 * <p>The core declares both fields and leaves them alone: what being tracked amounts to is a
 * matter for whoever draws a panel. Static so a feature can ask whenever it likes, without
 * depending on anything being set up first.
 *
 * <p>Tracking is answered, not stored: a set of its own would have to be kept in step with quests
 * arriving, ending and being done away with.
 */
public final class QuestTrackService {

    private QuestTrackService() {}

    /**
     * @return {@code false} if the quest was already tracked.
     */
    public static boolean track(@Nonnull AbstractQuestProgression<?> quest) {
        return apply(quest, Boolean.TRUE);
    }

    /**
     * @return {@code false} if the quest was already dropped.
     */
    public static boolean untrack(@Nonnull AbstractQuestProgression<?> quest) {
        return apply(quest, Boolean.FALSE);
    }

    /**
     * Hands the answer back to the asset, so a quest the player never had an opinion on reads the
     * way a freshly handed out one does.
     *
     * @return {@code false} if the asset was going to say the same thing anyway.
     */
    public static boolean reset(@Nonnull AbstractQuestProgression<?> quest) {
        return apply(quest, null);
    }

    /**
     * Tracks the quest if it is dropped and drops it if it is tracked, which is what a button
     * offering one control for both does.
     *
     * @return what the quest now says.
     */
    public static boolean toggle(@Nonnull AbstractQuestProgression<?> quest) {
        boolean tracked = !quest.isTracked();
        apply(quest, tracked);

        return tracked;
    }

    /**
     * Goes by what the quest ends up saying rather than by what was written on it: dropping an
     * override that agreed with the asset moves the quest without moving the answer, and neither
     * a panel nor anyone listening is concerned by that.
     *
     * @return whether the answer changed.
     */
    private static boolean apply(@Nonnull AbstractQuestProgression<?> quest, @Nullable Boolean track) {
        boolean was = quest.isTracked();
        quest.setTracked(track);

        boolean now = quest.isTracked();
        if (was == now) return false;

        announce(quest, now);
        return true;
    }

    /**
     * Fired for the quest, leaving whoever listens to work out which of its holders they care
     * about — tracking is a property of the quest, and every holder of it sees the same answer.
     */
    private static void announce(@Nonnull AbstractQuestProgression<?> quest, boolean tracked) {
        var eventBus = HytaleServer.get().getEventBus();

        if (tracked) {
            eventBus.dispatchFor(QuestTrackedEvent.class, quest.getId()).dispatch(new QuestTrackedEvent(quest));
            return;
        }

        eventBus.dispatchFor(QuestUntrackedEvent.class, quest.getId()).dispatch(new QuestUntrackedEvent(quest));
    }

    /**
     * @return whether the quest is tracked at all. A quest the player is not meant to read about
     * never is, whatever it or its asset asked for.
     */
    public static boolean isTracked(@Nonnull AbstractQuestProgression<?> quest) {
        if (!quest.isVisible()) return false;

        return quest.isTracked();
    }

    /**
     * @return whether the quest is tracked by this player right now. What a quest is tracked for
     * outlives the work, so asking without a player would call one tracked long after it ended.
     */
    public static boolean isTracked(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        return quest.getStateFor(playerId) == QuestState.IN_PROGRESS && isTracked(quest);
    }

    /**
     * Reads the player's stored data when they are offline, which blocks. Prefer the overload
     * taking their components wherever they are already at hand.
     *
     * @return the ids of the quests this player is tracking, in no particular order.
     */
    @Nonnull
    public static List<UUID> getTracked(@Nonnull UUID playerId) {
        return getTracked(playerId, EntityComponents.of(playerId));
    }

    /**
     * Ids rather than progressions: a caller setting the list aside for the length of a round would
     * otherwise be holding objects that a player logging out takes out of memory, and hand back
     * quests nothing reads any more.
     *
     * @return the ids of the quests this player is tracking, in no particular order.
     */
    @Nonnull
    public static List<UUID> getTracked(@Nonnull UUID playerId, @Nonnull EntityComponents playerComponents) {
        QuestStoreComponent questStore = playerComponents.getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return List.of();

        List<UUID> tracked = new ArrayList<>();

        for (UUID questId : questStore.getQuestIds()) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);

            if (quest != null && isTracked(quest, playerId)) tracked.add(questId);
        }
        return tracked;
    }

    /**
     * Drops every quest this player tracks and tracks the given ones instead, which is how a game
     * mode borrows the tracker for the length of a round.
     *
     * @param questIds what to track once the rest is dropped, empty to leave the tracker bare.
     * @return what was being tracked until now, to be handed back to this same method afterwards.
     */
    @Nonnull
    public static List<UUID> replaceTracked(@Nonnull UUID playerId, @Nullable List<UUID> questIds) {
        List<UUID> previous = getTracked(playerId);

        setTracked(previous, false);
        if (questIds != null) setTracked(questIds, true);

        return previous;
    }

    /**
     * Goes through {@link #track} and {@link #untrack} rather than writing the flag, so a round
     * borrowing the tracker is announced like anything else that changes it.
     *
     * <p>Skips what is no longer in memory: a quest read back in only to be marked and dropped
     * again would be marked on an instance nobody else holds.
     */
    private static void setTracked(@Nonnull List<UUID> questIds, boolean tracked) {
        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null) continue;

            if (tracked) track(quest); else untrack(quest);
        }
    }
}
