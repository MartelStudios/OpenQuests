package com.martelstudios.openquests.extension.track;

import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
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
 * {@code AutoTrack}, the player's own quest store carries what they made of it, and everything
 * drawing a quest as tracked asks here rather than reading either.
 *
 * <p>Tracking is the player's: a quest many players hold is tracked by each of them their own way.
 * Static so a feature can ask whenever it likes, without depending on anything being set up first.
 *
 * <p>Tracking is answered, not stored: a set of its own would have to be kept in step with quests
 * arriving, ending and being done away with.
 */
public final class QuestTrackService {

    private QuestTrackService() {}

    /**
     * @return {@code false} if the player already tracked the quest.
     */
    public static boolean track(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        return apply(quest, playerId, Boolean.TRUE);
    }

    /**
     * @return {@code false} if the player had already dropped the quest.
     */
    public static boolean untrack(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        return apply(quest, playerId, Boolean.FALSE);
    }

    /**
     * Hands the answer back to the asset, so a quest the player never had an opinion on reads the
     * way a freshly handed out one does.
     *
     * @return {@code false} if the asset was going to say the same thing anyway.
     */
    public static boolean reset(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        return apply(quest, playerId, null);
    }

    /**
     * Tracks the quest if the player dropped it and drops it if they track it, which is what a
     * button offering one control for both does.
     *
     * @return what the player now says.
     */
    public static boolean toggle(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        boolean tracked = !wants(quest, QuestPlayerStateService.get().getQuestStore(playerId));
        apply(quest, playerId, tracked);

        return tracked;
    }

    /**
     * Goes by what the player ends up saying rather than by what was written: dropping an override
     * that agreed with the asset moves nothing a panel or anyone listening is concerned by. A
     * player this server does not host has nothing here to write to.
     *
     * @return whether the answer changed.
     */
    private static boolean apply(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId, @Nullable Boolean track) {
        QuestStoreComponent questStore = QuestPlayerStateService.get().getQuestStore(playerId);
        if (questStore == null) return false;

        boolean was = wants(quest, questStore);
        questStore.setTracking(quest.getId(), track);

        boolean now = wants(quest, questStore);
        if (was == now) return false;

        announce(quest, playerId, now);
        return true;
    }

    private static void announce(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId, boolean tracked) {
        var eventBus = HytaleServer.get().getEventBus();

        if (tracked) {
            eventBus.dispatchFor(QuestTrackedEvent.class, quest.getId()).dispatch(new QuestTrackedEvent(quest, playerId));
            return;
        }

        eventBus.dispatchFor(QuestUntrackedEvent.class, quest.getId()).dispatch(new QuestUntrackedEvent(quest, playerId));
    }

    /**
     * @return whether the quest is tracked by this player right now. What a quest is tracked for
     * outlives the work, so a quest the player is done with never is, nor one they are not meant
     * to read about, whatever they or its asset asked for.
     */
    public static boolean isTracked(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        return isTracked(quest, playerId, QuestPlayerStateService.get().getQuestStore(playerId));
    }

    private static boolean isTracked(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId, @Nullable QuestStoreComponent questStore) {
        return quest.getStateFor(playerId) == QuestState.IN_PROGRESS && quest.isVisible() && wants(quest, questStore);
    }

    /**
     * @return what the player said of tracking the quest, or what its asset says while they said
     * nothing.
     */
    private static boolean wants(@Nonnull AbstractQuestProgression<?> quest, @Nullable QuestStoreComponent questStore) {
        Boolean said = questStore == null ? null : questStore.getTracking(quest.getId());
        if (said != null) return said;

        OpenQuestAsset asset = quest.getAsset();
        return asset != null && asset.isAutoTrack();
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

            if (quest != null && isTracked(quest, playerId, questStore)) tracked.add(questId);
        }
        return tracked;
    }

    /**
     * Drops every quest this player tracks and tracks the given ones instead, which is how a game
     * mode borrows the tracker for the length of a round. The other holders of those quests keep
     * theirs as they were.
     *
     * @param questIds what to track once the rest is dropped, empty to leave the tracker bare.
     * @return what was being tracked until now, to be handed back to this same method afterwards.
     */
    @Nonnull
    public static List<UUID> replaceTracked(@Nonnull UUID playerId, @Nullable List<UUID> questIds) {
        List<UUID> previous = getTracked(playerId);

        setTracked(playerId, previous, false);
        if (questIds != null) setTracked(playerId, questIds, true);

        return previous;
    }

    /**
     * Goes through {@link #track} and {@link #untrack} rather than writing the answer, so a round
     * borrowing the tracker is announced like anything else that changes it.
     *
     * <p>Skips what is no longer in memory: what its asset says and who hears of it need the quest.
     */
    private static void setTracked(@Nonnull UUID playerId, @Nonnull List<UUID> questIds, boolean tracked) {
        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null) continue;

            if (tracked) track(quest, playerId); else untrack(quest, playerId);
        }
    }
}
