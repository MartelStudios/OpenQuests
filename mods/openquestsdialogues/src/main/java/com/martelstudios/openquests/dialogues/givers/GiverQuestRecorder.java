package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * The one writer of {@link GiverQuestRecord}: keeps how the head quests an NPC handed out start
 * and end, for each player holding them, on that player's thread or in their stored data.
 */
public final class GiverQuestRecorder {

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
        GiverQuestRecorder recorder = new GiverQuestRecorder(type);
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, recorder::onAdded);
        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, recorder::onCompleted);
        return type;
    }

    private void onAdded(@Nonnull QuestAddedToPlayerStoreEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        Giver giver = headGiver(quest);
        if (giver == null) return;

        Instant at = timeOr(quest.getStartedAt());
        String assetId = quest.getAssetId();
        EntityComponents.update(event.getPlayerId(), components -> components.ensureAndGetComponent(type).started(giver, assetId, at));
    }

    /**
     * Counted as core counts its own tally: once, by the server ending the quest, those who gave
     * it up as having abandoned it, so a player coming back and seeing it through counts once.
     */
    private void onCompleted(@Nonnull QuestCompletedEvent event) {
        if (!event.isEndedHere()) return;
        AbstractQuestProgression<?> quest = event.getQuest();
        Giver giver = headGiver(quest);
        if (giver == null) return;

        Instant at = timeOr(quest.getCompletedAt());
        String assetId = quest.getAssetId();
        for (UUID playerId : quest.getPlayers()) record(playerId, giver, assetId, event.getState(), at);
        for (UUID playerId : quest.getAbandonedPlayers()) record(playerId, giver, assetId, QuestState.ABANDONED, at);
    }

    private void record(@Nonnull UUID playerId, @Nonnull Giver giver, @Nonnull String assetId, @Nonnull QuestState state, @Nonnull Instant at) {
        EntityComponents.update(playerId, components -> components.ensureAndGetComponent(type).ended(giver, assetId, state, at));
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
