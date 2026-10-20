package com.martelstudios.openquests.core;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStoreComponent;
import com.martelstudios.openquests.core.stores.PlayerComponentTypes;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.utils.Players;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Players kept in memory, online or not, each with the components the services keep on them. A
 * write reaches them at once, where the server would queue it on their world thread.
 */
public final class TestPlayers implements Players {

    private final PlayerComponentTypes types = new PlayerComponentTypes(new ComponentType<>(), new ComponentType<>());

    private final Set<UUID> online = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Player> players = new ConcurrentHashMap<>();

    @Nonnull
    public PlayerComponentTypes types() {
        return types;
    }

    /**
     * @return a new player, connected.
     */
    @Nonnull
    public UUID connect() {
        UUID playerId = UUID.randomUUID();
        online.add(playerId);
        return playerId;
    }

    /**
     * Takes the player off the server, their components kept as stored data.
     */
    public void disconnect(@Nonnull UUID playerId) {
        online.remove(playerId);
    }

    /**
     * @return the player's components, as the services left them.
     */
    @Nonnull
    public EntityComponents of(@Nonnull UUID playerId) {
        return players.computeIfAbsent(playerId, id -> new Player());
    }

    @Nonnull
    private Component<EntityStore> create(@Nonnull ComponentType<EntityStore, ?> type) {
        if (type == types.questStore()) return new QuestStoreComponent();
        if (type == types.pendingRewards()) return new PendingRewardStoreComponent();
        throw new IllegalArgumentException("no component of that type is kept on a test player");
    }

    @Override
    public boolean isOnline(@Nonnull UUID playerId) {
        return online.contains(playerId);
    }

    @Nonnull
    @Override
    public Collection<UUID> online() {
        return List.copyOf(online);
    }

    @Override
    public void update(@Nonnull UUID playerId, @Nonnull Consumer<EntityComponents> action) {
        action.accept(of(playerId));
    }

    private final class Player implements EntityComponents {
        // By identity: component types the entity store never registered all compare equal
        private final Map<ComponentType<EntityStore, ?>, Component<EntityStore>> components = new IdentityHashMap<>();

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public synchronized <T extends Component<EntityStore>> T getComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
            return (T) components.get(componentType);
        }

        @Nonnull
        @Override
        @SuppressWarnings("unchecked")
        public synchronized <T extends Component<EntityStore>> T ensureAndGetComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
            return (T) components.computeIfAbsent(componentType, TestPlayers.this::create);
        }

        @Override
        public synchronized <T extends Component<EntityStore>> boolean removeComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
            return components.remove(componentType) != null;
        }

        @Nullable
        @Override
        public Ref<EntityStore> getReference() {
            return null;
        }
    }
}
