package com.martelstudios.openquests.extension.listener;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The quests of one kind a player is running, held on the player themselves. A ticking system
 * concerned with those quests queries for this component instead of walking every player and every
 * quest they hold: an entity carrying none is not in the query at all.
 *
 * <p>Never persisted, and built back on connection — which is also what makes it impossible to
 * leak, since it goes wherever the player's entity goes.
 *
 * <p>Concrete subtypes add nothing. One class per kind is what buys the filtering: a query asks
 * whether a component is there, never what is inside it.
 */
public abstract class QuestListenerComponent implements Component<EntityStore> {

    private final Set<UUID> questIds = ConcurrentHashMap.newKeySet();

    /**
     * @return the live set of quest ids to progress.
     */
    @Nonnull
    public Set<UUID> getQuestIds() {
        return questIds;
    }

    /**
     * @return {@code false} if the quest was already followed.
     */
    public boolean follow(@Nonnull UUID questId) {
        return questIds.add(questId);
    }

    /**
     * @return {@code false} if the quest was not followed.
     */
    public boolean forget(@Nonnull UUID questId) {
        return questIds.remove(questId);
    }

    /**
     * @return {@code true} once nothing is left to tick, which is when the component is let go of.
     */
    public boolean isEmpty() {
        return questIds.isEmpty();
    }

    /**
     * @return an empty component of this same kind, which is all a clone needs to be filled from.
     */
    @Nonnull
    protected abstract QuestListenerComponent create();

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        QuestListenerComponent copy = create();
        copy.questIds.addAll(questIds);

        return copy;
    }
}
