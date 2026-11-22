package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The one writer of {@link GiverQuestRecord}: keeps how the head quests an NPC handed out start
 * and end, for each player holding them, on that player's thread or in their stored data.
 */
public final class GiverQuestRecorder {

    private static volatile ComponentType<EntityStore, GiverQuestRecord> registered;

    private final ComponentType<EntityStore, GiverQuestRecord> type;

    private GiverQuestRecorder(@Nonnull ComponentType<EntityStore, GiverQuestRecord> type) {
        this.type = type;
    }

    /**
     * Registers the record component and listens to quests starting and ending.
     *
     * @return the record's component type, for the conditions reading it.
     */
    @Nonnull
    public static ComponentType<EntityStore, GiverQuestRecord> register(@Nonnull JavaPlugin plugin) {
        ComponentType<EntityStore, GiverQuestRecord> type = plugin.getEntityStoreRegistry().registerComponent(GiverQuestRecord.class, "OpenQuestsDialoguesGivers", GiverQuestRecord.CODEC);
        registered = type;
        GiverQuestRecorder recorder = new GiverQuestRecorder(type);
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, recorder::onAdded);
        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, recorder::onCompleted);
        return type;
    }

    /**
     * @return the record's component type, {@code null} before the plugin set up.
     */
    @Nullable
    public static ComponentType<EntityStore, GiverQuestRecord> type() {
        return registered;
    }

    private void onAdded(@Nonnull QuestAddedToPlayerStoreEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        Giver giver = headGiver(quest);
        if (giver == null) return;

        Instant at = timeOr(quest.getStartedAt());
        String assetId = quest.getAssetId();
        write(event.getPlayerId(), record -> record.started(giver, assetId, at));
    }

    private void onCompleted(@Nonnull QuestCompletedEvent event) {
        if (!event.isEndedHere()) return;
        AbstractQuestProgression<?> quest = event.getQuest();
        Giver giver = headGiver(quest);
        if (giver == null) return;

        Instant at = timeOr(quest.getCompletedAt());
        String assetId = quest.getAssetId();
        outcomes(event).forEach((playerId, state) -> write(playerId, record -> record.ended(giver, assetId, state, at)));
    }

    /**
     * Counted as core counts its own tally: once, by the server ending the quest, for the team
     * snapshot core took, those who gave it up as having abandoned it.
     *
     * @return the outcome to count for each player.
     */
    @Nonnull
    static Map<UUID, QuestState> outcomes(@Nonnull QuestCompletedEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        Map<UUID, QuestState> outcomes = new HashMap<>();
        for (UUID playerId : event.recipients(() -> Set.copyOf(quest.getPlayers()))) outcomes.put(playerId, event.getState());
        for (UUID playerId : quest.getAbandonedPlayers()) outcomes.put(playerId, QuestState.ABANDONED);
        return outcomes;
    }

    /**
     * On the player's world thread the write is made at once, so a condition checked right after
     * a quest ends in the same tick already reads it: the live tally no longer counts an ended quest.
     */
    private void write(@Nonnull UUID playerId, @Nonnull Consumer<GiverQuestRecord> action) {
        PlayerRef online = Universe.get().getPlayer(playerId);
        Ref<EntityStore> ref = online == null ? null : online.getReference();
        GiverQuestRecord here = ref != null && ref.isValid() && ref.getStore().isInThread() ? ref.getStore().ensureAndGetComponent(ref, type) : null;
        write(here, action, () -> EntityComponents.update(playerId, components -> action.accept(components.ensureAndGetComponent(type))));
    }

    /**
     * @param here the record when it can be written from this thread, {@code null} otherwise.
     * @param queue writes it later where it can.
     */
    static void write(@Nullable GiverQuestRecord here, @Nonnull Consumer<GiverQuestRecord> action, @Nonnull Runnable queue) {
        if (here != null) action.accept(here);
        else queue.run();
    }

    /**
     * @return the giver of a head quest with an asset, {@code null} for a step or a quest no NPC handed out.
     */
    @Nullable
    private static Giver headGiver(@Nonnull AbstractQuestProgression<?> quest) {
        return quest.getParentId() == null && quest.getAssetId() != null ? Giver.of(quest) : null;
    }

    @Nonnull
    private static Instant timeOr(@Nullable Instant time) {
        return time != null ? time : Instant.now();
    }
}
