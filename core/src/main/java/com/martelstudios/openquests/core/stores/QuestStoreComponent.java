package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The quests of one player while they are online: which ones they take part in, and which of the
 * catalogue they have already been offered.
 *
 * <p>Never written to the player's entity file: where a quest is kept is the
 * {@link com.martelstudios.openquests.core.persistence.QuestStorage}'s business, and this is the
 * reverse index it fills on connection.
 *
 * <p>It stays a component because it is the marker every quest system queries on.
 */
public class QuestStoreComponent implements Component<EntityStore> {

    private QuestsRecord quests = new QuestsRecord();

    /**
     * Asset ids already handed to this player by {@code StartOnConnection}. Only these are kept
     * between sessions, not the quests made from them, so a catalogue offered to everyone costs
     * one string per quest actually taken.
     */
    private final Set<String> startedOnConnection = ConcurrentHashMap.newKeySet();

    /**
     * Set when something here changed and the player's record is owed a write.
     */
    private transient boolean dirty;

    public QuestStoreComponent() {

    }

    public QuestStoreComponent(@Nonnull QuestStoreComponent other) {
        this.quests = other.quests.clone();
        this.startedOnConnection.addAll(other.startedOnConnection);
        this.dirty = other.dirty;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new QuestStoreComponent(this);
    }

    public static ComponentType<EntityStore, QuestStoreComponent> getComponentType() {
        return OpenQuestsCorePlugin.get().getQuestStoreComponentType();
    }

    /**
     * @return the index of every quest of this player.
     */
    @Nonnull
    public QuestsRecord getQuests() {
        return quests;
    }

    /**
     * @return the live set of ids of every quest of this player.
     */
    @Nonnull
    public Set<UUID> getQuestIds() {
        return quests.getAllIds();
    }

    @Nonnull
    public Set<String> getStartedOnConnection() {
        return startedOnConnection;
    }

    /**
     * Takes over what was read back for this player, which is how a session starts.
     *
     * @param questIds the quests that answered, not the ids their record listed, so a dead id is
     * dropped here rather than carried another session.
     */
    public void restore(@Nonnull Set<UUID> questIds, @Nonnull Set<String> startedOnConnection) {
        quests.replaceAll(questIds);
        this.startedOnConnection.addAll(startedOnConnection);
        dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

    /**
     * @return {@code true} if this player's record changed since the last write, without clearing
     * the flag.
     */
    public boolean hasChanges() {
        return dirty;
    }

    /**
     * @return {@code true} if this player's record changed since the last call, clearing the flag.
     */
    public boolean consumeChanges() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }
}
