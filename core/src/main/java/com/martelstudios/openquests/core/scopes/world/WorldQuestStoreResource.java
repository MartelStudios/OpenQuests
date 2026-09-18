package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Per-world index of quest ids shared by every player of that world. Mirrors
 * {@link QuestStoreComponent} (per-player), but lives on the world's {@code EntityStore} as a
 * {@link Resource} since it isn't owned by a single entity.
 *
 * <p>In memory only, like its per-player counterpart. The index is read from the storage under
 * {@code world:<uuid>}, so a world holds the same quests on every server sharing it.
 */
public class WorldQuestStoreResource implements Resource<EntityStore> {

    public QuestsRecord questsRecord = new QuestsRecord();

    /**
     * Whether the index still has to be read back. No world-load event reaches this plugin, so
     * the first player through the door pays for it.
     */
    private boolean loaded;

    private boolean dirty;

    public WorldQuestStoreResource() {
    }

    public WorldQuestStoreResource(@Nonnull WorldQuestStoreResource other) {
        this.questsRecord = other.questsRecord.clone();
        this.loaded = other.loaded;
        this.dirty = other.dirty;
    }

    @Nullable
    @Override
    public Resource<EntityStore> clone() {
        return new WorldQuestStoreResource(this);
    }

    public static ResourceType<EntityStore, WorldQuestStoreResource> getResourceType() {
        return OpenQuestsCorePlugin.get().getWorldStoreResourceType();
    }

    /**
     * @return {@code true} the first time this is called for this world, {@code false} afterwards.
     */
    public boolean consumeNeedsLoad() {
        if (loaded) return false;
        loaded = true;
        return true;
    }

    public void markDirty() {
        this.dirty = true;
    }

    /**
     * @return {@code true} if this world's index changed since the last call, clearing the flag.
     */
    public boolean consumeChanges() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }
}
