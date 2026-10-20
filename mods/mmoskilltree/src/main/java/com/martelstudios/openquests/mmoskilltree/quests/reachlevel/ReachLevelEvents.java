package com.martelstudios.openquests.mmoskilltree.quests.reachlevel;

import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.Players;
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
    private final Consumer<PlayerGainXpEvent> xpListener = this::handleXpGained;

    private final QuestProgressionService progression;
    private final Players players;

    public ReachLevelEvents(@Nonnull QuestProgressionService progression, @Nonnull Players players) {
        this.progression = progression;
        this.players = players;
    }

    /**
     * @return the listener to add to MMO Skill Tree, and to take back by the same identity.
     */
    @Nonnull
    public Consumer<PlayerGainXpEvent> xpListener() {
        return xpListener;
    }

    /**
     * Fired by MMO Skill Tree as it awards the XP, on the player's world thread. Only a change of
     * level moves these quests.
     */
    private void handleXpGained(@Nonnull PlayerGainXpEvent event) {
        if (event.getLevelBefore() == event.getLevelAfter()) return;

        var playerRef = event.getPlayerRef();
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        var questStore = ref.getStore().getComponent(ref, QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        progression.progress(new ReachLevelQuestVisitor(playerRef.getUuid(), ref), questStore.getQuestIds());
    }

    /**
     * Catches a freshly assigned quest up with the level the player already has, so one asking for
     * a level they reached long ago completes at once.
     */
    public void handleQuestAssignedToPlayer(@Nonnull QuestAddedToPlayerStoreEvent event) {
        if (!(event.getQuest() instanceof ReachLevelQuestProgression quest)) return;

        // ponytail: a player held out of world is caught up on their next level change instead
        players.update(event.getPlayerId(), components -> {
            var ref = components.getReference();
            if (ref != null) quest.update(new ReachLevelQuestVisitor(event.getPlayerId(), ref));
        });
    }
}
