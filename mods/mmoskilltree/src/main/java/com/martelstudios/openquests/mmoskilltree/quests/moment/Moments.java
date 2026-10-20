package com.martelstudios.openquests.mmoskilltree.quests.moment;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Hands one moment to the player's quests of a type. MMO Skill Tree tells its own quests of these
 * moments directly rather than through CommonLib's dispatch, so each type hears them from where they
 * happen and comes through here.
 */
public final class Moments {

    private final QuestProgressionService progression;

    public Moments(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    /**
     * Runs on the player's world thread, which every source of moments is on.
     */
    public <Q extends MomentQuestProgression<Q>> void count(@Nonnull Ref<EntityStore> playerRef, @Nonnull UUID playerId, @Nonnull Class<Q> type,
                                                                    @Nonnull String target, @Nullable String qualifier, long amount) {
        if (amount <= 0 || !playerRef.isValid()) return;

        var questStore = playerRef.getStore().getComponent(playerRef, QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        progression.progress(new Visitor<>(playerId, type, target, qualifier, (int) Math.min(amount, Integer.MAX_VALUE)), questStore.getQuestIds());
    }

    private record Visitor<Q extends MomentQuestProgression<Q>>(@Nonnull UUID playerId, @Nonnull Class<Q> type, @Nonnull String target,
                                                               @Nullable String qualifier, int amount) implements QuestVisitor<Q> {
        @Override
        public void progress(Q quest) {
            if (quest.getPlayers().contains(playerId) && quest.matches(target, qualifier)) quest.apply(new QuantityQuestProgression.Add(amount));
        }

        @Override
        public Class<Q> getQuestType() {
            return type;
        }

        @Nonnull
        @Override
        public UUID getActorId() {
            return playerId;
        }
    }
}
