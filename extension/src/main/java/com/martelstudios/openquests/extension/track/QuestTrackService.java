package com.martelstudios.openquests.extension.track;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.extension.tags.OpenQuestsTags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Which quests a player is following. The asset says what a quest starts out as through
 * {@code AutoTrack}, the quest itself carries what the player made of it, and everything drawing a
 * quest as followed asks here rather than reading either.
 *
 * <p>The core declares both fields and leaves them alone: what being followed amounts to is a
 * matter for whoever draws a panel. Static so a feature can ask whenever it likes, without
 * depending on anything being set up first.
 *
 * <p>Tracking is answered, not stored: a set of its own would have to be kept in step with quests
 * arriving, ending and being done away with.
 */
public final class QuestTrackService {

    private QuestTrackService() {}

    /**
     * @return {@code false} if the quest was already followed.
     */
    public static boolean track(@Nonnull AbstractQuestProgression<?> quest) {
        return quest.setTracked(Boolean.TRUE);
    }

    /**
     * @return {@code false} if the quest was already dropped.
     */
    public static boolean untrack(@Nonnull AbstractQuestProgression<?> quest) {
        return quest.setTracked(Boolean.FALSE);
    }

    /**
     * Hands the answer back to the asset, so a quest the player never had an opinion on reads the
     * way a freshly handed out one does.
     *
     * @return {@code false} if the quest had no opinion of its own to drop.
     */
    public static boolean reset(@Nonnull AbstractQuestProgression<?> quest) {
        return quest.setTracked(null);
    }

    /**
     * Follows the quest if it is dropped and drops it if it is followed, which is what a button
     * offering one control for both does.
     *
     * @return what the quest now says.
     */
    public static boolean toggle(@Nonnull AbstractQuestProgression<?> quest) {
        boolean tracked = !quest.isTracked();
        quest.setTracked(tracked);

        return tracked;
    }

    /**
     * @return whether the quest is followed at all. A quest the player is not meant to read about
     * never is, whatever it or its asset asked for.
     */
    public static boolean isTracked(@Nonnull AbstractQuestProgression<?> quest) {
        if (quest.hasTag(OpenQuestsTags.HIDE_TAG)) return false;

        return quest.isTracked();
    }

    /**
     * @return whether the quest is followed by this player right now. What a quest is followed for
     * outlives the work, so asking without a player would call one followed long after it ended.
     */
    public static boolean isTracked(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        return quest.getStateFor(playerId) == QuestState.IN_PROGRESS && isTracked(quest);
    }

    /**
     * Reads the player's stored data when they are offline, which blocks. Prefer the overload
     * taking their components wherever they are already at hand.
     *
     * @return the ids of the quests this player is following, in no particular order.
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
     * @return the ids of the quests this player is following, in no particular order.
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
     * Drops every quest this player follows and follows the given ones instead, which is how a game
     * mode borrows the tracker for the length of a round.
     *
     * @param questIds what to follow once the rest is dropped, empty to leave the tracker bare.
     * @return what was being followed until now, to be handed back to this same method afterwards.
     */
    @Nonnull
    public static List<UUID> replaceTracked(@Nonnull UUID playerId, @Nullable List<UUID> questIds) {
        List<UUID> previous = getTracked(playerId);

        setTracked(previous, false);
        if (questIds != null) setTracked(questIds, true);

        return previous;
    }

    /**
     * Skips what is no longer in memory: a quest read back in only to be marked and dropped again
     * would be marked on an instance nobody else holds.
     */
    private static void setTracked(@Nonnull List<UUID> questIds, boolean tracked) {
        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);

            if (quest != null) quest.setTracked(tracked);
        }
    }
}
