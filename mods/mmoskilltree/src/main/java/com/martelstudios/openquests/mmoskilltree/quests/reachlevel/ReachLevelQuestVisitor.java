package com.martelstudios.openquests.mmoskilltree.quests.reachlevel;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Sets each quest's count to the player's level in its skill. Reads the player's store, so it runs
 * on their world thread.
 */
public class ReachLevelQuestVisitor implements QuestVisitor<ReachLevelQuestProgression> {
    private final UUID playerId;
    private final Ref<EntityStore> playerRef;

    public ReachLevelQuestVisitor(@Nonnull UUID playerId, @Nonnull Ref<EntityStore> playerRef) {
        this.playerId = playerId;
        this.playerRef = playerRef;
    }

    @Override
    public void progress(ReachLevelQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;

        int level = MMOSkillTreeAPI.getLevel(playerRef.getStore(), playerRef, quest.getSkill());
        quest.apply(new QuantityQuestProgression.Count(level));
    }

    @Override
    public Class<ReachLevelQuestProgression> getQuestType() {
        return ReachLevelQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
