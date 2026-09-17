package com.martelstudios.openquests.extension.listener;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Where a quest type declares that the quests a player runs of it are worth naming on the player.
 * Keeps every {@link QuestListenerComponent} in step with the quest store, so that a ticking
 * system reaches only the players it concerns and only the quests it can progress.
 */
public final class QuestListenerService {

    /**
     * One quest type and the component naming a player's quests of it. A list rather than a map:
     * a kind is matched on {@code isInstance}, so a base type covers everything built on it.
     */
    private record Kind(@Nonnull Class<?> questType, @Nonnull ComponentType<EntityStore, ? extends QuestListenerComponent> componentType) {}

    private static final List<Kind> KINDS = new CopyOnWriteArrayList<>();

    private QuestListenerService() {}

    /**
     * @param questType matched on {@code isInstance}, so registering a base type covers every type
     * built on it — which is what lets one ticking system serve a family of quests.
     */
    public static void register(@Nonnull Class<?> questType, @Nonnull ComponentType<EntityStore, ? extends QuestListenerComponent> componentType) {
        KINDS.add(new Kind(questType, componentType));
    }

    /**
     * Builds every listener from scratch. Nothing can be written to a connecting player by id —
     * they are not online yet — so this pass goes through their incoming holder, and it runs last
     * so that whatever connecting hands out has been handed out.
     */
    public static void handlePlayerConnect(@Nonnull PlayerConnectEvent event) {
        var components = EntityComponents.of(event.getHolder());

        QuestStoreComponent questStore = components.getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        for (UUID questId : questStore.getQuestIds()) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getLiveQuest(questId);
            if (quest == null) continue;

            var componentType = componentTypeFor(quest);
            if (componentType == null) continue;

            components.ensureAndGetComponent(componentType).follow(questId);
        }
    }

    public static void handleQuestAddedToPlayerStore(@Nonnull QuestAddedToPlayerStoreEvent event) {
        follow(event.getQuest(), event.getPlayerId());
    }

    public static void handleQuestRemovedFromPlayerStore(@Nonnull QuestRemovedFromPlayerStoreEvent event) {
        forget(event.getQuest(), event.getPlayerId());
    }

    /**
     * A quest that ended stops being ticked, so it stops being listed — without which the list
     * would only ever grow, one entry per quest of that kind the player ever finished.
     *
     * <p>Archiving happens before the change is announced, which is what makes "no longer live"
     * the whole test: a quest kept running by {@code StopOnComplete: false} stays listed.
     */
    public static void handleQuestStateChanged(@Nonnull QuestStateChangedEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();
        if (QuestProgressionService.get().getLiveQuest(quest.getId()) != null) return;

        quest.getPlayers().forEach(playerId -> forget(quest, playerId));
        quest.getAbandonedPlayers().forEach(playerId -> forget(quest, playerId));
    }

    private static void follow(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        var componentType = componentTypeFor(quest);
        if (componentType == null || isOffline(playerId)) return;

        EntityComponents.update(playerId, components -> components.ensureAndGetComponent(componentType).follow(quest.getId()));
    }

    private static void forget(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        var componentType = componentTypeFor(quest);
        if (componentType == null || isOffline(playerId)) return;

        EntityComponents.update(playerId, components -> {
            QuestListenerComponent listener = components.getComponent(componentType);
            if (listener == null || !listener.forget(quest.getId())) return;

            // Taken off rather than left empty, which is what puts the player back out of the query
            // instead of having them ticked over nothing
            if (listener.isEmpty()) components.removeComponent(componentType);
        });
    }

    /**
     * Nothing ticks for a player who is not there, and writing to their stored data would be a disk
     * read and a disk write for a component that is never written down.
     */
    private static boolean isOffline(@Nonnull UUID playerId) {
        return Universe.get().getPlayer(playerId) == null;
    }

    @Nullable
    private static ComponentType<EntityStore, ? extends QuestListenerComponent> componentTypeFor(@Nonnull AbstractQuestProgression<?> quest) {
        for (Kind kind : KINDS) {
            if (kind.questType().isInstance(quest)) return kind.componentType();
        }
        return null;
    }
}
