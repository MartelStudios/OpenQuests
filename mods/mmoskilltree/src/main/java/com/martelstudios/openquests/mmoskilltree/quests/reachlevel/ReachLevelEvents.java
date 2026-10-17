package com.martelstudios.openquests.mmoskilltree.quests.reachlevel;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.mmoskilltree.api.events.PlayerGainXpEvent;
import com.ziggfreed.mmoskilltree.skill.SkillRegistry;

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

    /**
     * Fails the load once per asset naming a skill MMO Skill Tree does not know.
     */
    public static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            if (asset instanceof MMOSkillTreeReachLevelQuestAsset reachLevel && !SkillRegistry.getInstance().isKnownSkill(reachLevel.getSkill())) {
                event.failed(true, "Quest asset '" + asset.getId() + "' names an unknown MMO Skill Tree skill '" + reachLevel.getSkill() + "'");
            }
        }
    }
}
