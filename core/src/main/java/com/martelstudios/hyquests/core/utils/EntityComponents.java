package com.martelstudios.hyquests.core.utils;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * One way to reach a player's components, whether they live in a store, in a holder out of world,
 * or on disk. Written against this, code stops needing an overload per form.
 */
public interface EntityComponents {

    @Nullable
    <T extends Component<EntityStore>> T getComponent(@Nonnull ComponentType<EntityStore, T> componentType);

    @Nonnull
    <T extends Component<EntityStore>> T ensureAndGetComponent(@Nonnull ComponentType<EntityStore, T> componentType);

    /**
     * @return the entity, or {@code null} when the player is held out of world. Only for the rare
     * code that genuinely needs a store, such as running on its world thread.
     */
    @Nullable
    Ref<EntityStore> getReference();

    @Nonnull
    static EntityComponents of(@Nonnull Holder<EntityStore> holder) {
        return new HeldComponents(holder);
    }

    @Nonnull
    static EntityComponents of(@Nonnull Ref<EntityStore> playerRef) {
        return new StoredComponents(playerRef);
    }

    /**
     * @return {@code null} if the player is neither in a store nor in a holder, which happens
     * while they are being handed from one to the other.
     */
    @Nullable
    static EntityComponents of(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> reference = playerRef.getReference();
        if (reference != null) return of(reference);

        Holder<EntityStore> holder = playerRef.getHolder();
        return holder == null ? null : of(holder);
    }

    /**
     * @return the online player's components, or {@code null} if they are offline. Never reads
     * from disk, so this is the one to prefer on a world thread.
     */
    @Nullable
    static EntityComponents ofOnline(@Nonnull UUID playerId) {
        PlayerRef online = Universe.get().getPlayer(playerId);
        return online == null ? null : of(online);
    }

    /**
     * Falls back to the stored data for an offline player, which is a blocking disk read. Writes
     * to it are dropped: go through {@link #update} for anything that must be kept.
     */
    @Nonnull
    static EntityComponents of(@Nonnull UUID playerId) {
        EntityComponents cached = PlayerComponentsCache.get(playerId);
        if (cached != null) return cached;

        EntityComponents online = ofOnline(playerId);
        EntityComponents components = online != null ? online : of(Universe.get().getPlayerStorage().load(playerId).join());

        PlayerComponentsCache.put(playerId, components);
        return components;
    }

    /**
     * Caches {@link #of(UUID)} if needed to often avoid disk loads.
     */
    @Nonnull
    static PlayerComponentsCache cache() {
        return new PlayerComponentsCache();
    }

    /**
     * Opened with try-with-resources. Caches can be nested, only the outermost one clears.
     */
    final class PlayerComponentsCache implements AutoCloseable {

        private static final ThreadLocal<Map<UUID, EntityComponents>> ACTIVE = new ThreadLocal<>();

        private final boolean outermost;

        private PlayerComponentsCache() {
            this.outermost = ACTIVE.get() == null;
            if (outermost) ACTIVE.set(new HashMap<>());
        }

        @Nullable
        private static EntityComponents get(@Nonnull UUID playerId) {
            Map<UUID, EntityComponents> active = ACTIVE.get();
            return active == null ? null : active.get(playerId);
        }

        private static void put(@Nonnull UUID playerId, @Nonnull EntityComponents components) {
            Map<UUID, EntityComponents> active = ACTIVE.get();
            if (active != null) active.put(playerId, components);
        }

        @Override
        public void close() {
            if (outermost) ACTIVE.remove();
        }
    }

    /**
     * Runs where the writes survives: the player's world thread, their holder, or a load-save pass
     * over their stored data. Asynchronous in every case but the holder.
     */
    static void update(@Nonnull UUID playerId, @Nonnull Consumer<EntityComponents> action) {
        PlayerRef online = Universe.get().getPlayer(playerId);

        if (online != null) {
            Ref<EntityStore> reference = online.getReference();
            if (reference != null) {
                reference.getStore().getExternalData().getWorld().execute(() -> action.accept(of(reference)));
                return;
            }

            Holder<EntityStore> holder = online.getHolder();
            if (holder != null) {
                action.accept(of(holder));
                return;
            }
        }

        Universe.get().getPlayerStorage().update(playerId, holder -> action.accept(of(holder)));
    }

    record HeldComponents(@Nonnull Holder<EntityStore> holder) implements EntityComponents {

        @Nullable
        @Override
        public <T extends Component<EntityStore>> T getComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
            return holder.getComponent(componentType);
        }

        @Nonnull
        @Override
        public <T extends Component<EntityStore>> T ensureAndGetComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
            return holder.ensureAndGetComponent(componentType);
        }

        @Nullable
        @Override
        public Ref<EntityStore> getReference() {
            return null;
        }
    }

    record StoredComponents(@Nonnull Ref<EntityStore> reference) implements EntityComponents {

        @Nullable
        @Override
        public <T extends Component<EntityStore>> T getComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
            return reference.getStore().getComponent(reference, componentType);
        }

        @Nonnull
        @Override
        public <T extends Component<EntityStore>> T ensureAndGetComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
            return reference.getStore().ensureAndGetComponent(reference, componentType);
        }

        @Nonnull
        @Override
        public Ref<EntityStore> getReference() {
            return reference;
        }
    }
}
