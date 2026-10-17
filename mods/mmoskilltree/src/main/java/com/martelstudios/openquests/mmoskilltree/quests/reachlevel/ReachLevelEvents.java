package com.martelstudios.openquests.mmoskilltree.quests.reachlevel;

import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.mmoskilltree.api.events.PlayerGainXpEvent;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Keeps {@link ReachLevelQuestProgression}s on the player's level.
 */
public final class ReachLevelEvents {

    /**
     * Held so the very listener added can be removed: MMO Skill Tree keeps its listeners by identity.
     */
    public static final Consumer<PlayerGainXpEvent> XP_LISTENER = ReachLevelEvents::handleXpGained;

    private ReachLevelEvents() {}

    /**
     * Fired by MMO Skill Tree as it awards the XP, on the player's world thread. Only a change of
     * level moves these quests.
     */
    private static void handleXpGained(@Nonnull PlayerGainXpEvent event) {
        if (event.getLevelBefore() == event.getLevelAfter()) return;

        var playerRef = event.getPlayerRef();
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        var questStore = ref.getStore().getComponent(ref, QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        QuestProgressionService.get().progress(new ReachLevelQuestVisitor(playerRef.getUuid(), ref), questStore.getQuestIds());
    }

    /**
     * Catches a freshly assigned quest up with the level the player already has, so one asking for
     * a level they reached long ago completes at once.
     */
    public static void handleQuestAssignedToPlayer(@Nonnull QuestAddedToPlayerStoreEvent event) {
        if (!(event.getQuest() instanceof ReachLevelQuestProgression quest)) return;

        // ponytail: a player held out of world is caught up on their next level change instead
        EntityComponents.update(event.getPlayerId(), components -> {
            var ref = components.getReference();
            if (ref != null) quest.update(new ReachLevelQuestVisitor(event.getPlayerId(), ref));
        });
    }
}
