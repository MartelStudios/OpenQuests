package com.martelstudios.openquests.mmoskilltree.quests.moment;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.common.quest.event.QuestClaimedEvent;
import com.ziggfreed.mmoskilltree.ability.cast.AbilityCastObserverRegistry;
import com.ziggfreed.mmoskilltree.ability.cast.PlayerAbilityUsedEvent;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;
import com.ziggfreed.mmoskilltree.api.events.PlayerGainXpEvent;
import com.ziggfreed.mmoskilltree.quest.MmoBounties;
import com.ziggfreed.mmoskilltree.skill.SkillRegistry;
import com.ziggfreed.mmoskilltree.skilltree.SkillTreeService;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Gain XP, cast an ability, collect a bounty: the moment quests, each fed from where MMO Skill Tree
 * makes the moment happen.
 */
public final class MomentFeature {
    public static final String GAIN_XP_TYPE_ID = "MMOSkillTreeGainXp";
    public static final String ABILITY_CAST_TYPE_ID = "MMOSkillTreeAbilityCast";
    public static final String COMPLETE_BOUNTY_TYPE_ID = "MMOSkillTreeCompleteBounty";

    /**
     * Held so the very listener added can be removed: MMO Skill Tree keeps its listeners by identity.
     */
    private static final Consumer<PlayerGainXpEvent> XP_LISTENER = MomentFeature::handleXpGained;

    /**
     * Cast observers cannot be removed, so a plugin set up again must not add a second one.
     */
    private static final AtomicBoolean CAST_OBSERVED = new AtomicBoolean();

    private MomentFeature() {}

    /**
     * Registers the three types and the sources of their moments.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get().registerQuestType(GAIN_XP_TYPE_ID, MMOSkillTreeGainXpQuestAsset.class, MMOSkillTreeGainXpQuestAsset.CODEC, GainXpQuestProgression.class, GainXpQuestProgression.CODEC);
        QuestProgressionService.get().registerQuestType(ABILITY_CAST_TYPE_ID, MMOSkillTreeAbilityCastQuestAsset.class, MMOSkillTreeAbilityCastQuestAsset.CODEC, AbilityCastQuestProgression.class, AbilityCastQuestProgression.CODEC);
        QuestProgressionService.get().registerQuestType(COMPLETE_BOUNTY_TYPE_ID, MMOSkillTreeCompleteBountyQuestAsset.class, MMOSkillTreeCompleteBountyQuestAsset.CODEC, CompleteBountyQuestProgression.class, CompleteBountyQuestProgression.CODEC);

        MMOSkillTreeAPI.addXpListener(XP_LISTENER);
        if (CAST_OBSERVED.compareAndSet(false, true)) AbilityCastObserverRegistry.getInstance().register(MomentFeature::handleAbilityCast);
        plugin.getEventRegistry().registerGlobal(QuestClaimedEvent.class, MomentFeature::handleQuestClaimed);
        plugin.getEventRegistry().registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, MomentFeature::handleLoadAsset);
    }

    /**
     * MMO Skill Tree's XP listeners outlive a plugin that shuts down, so this one is taken back by hand.
     */
    public static void unregister() {
        MMOSkillTreeAPI.removeXpListener(XP_LISTENER);
    }

    /**
     * Fired as the XP is awarded, on the player's world thread, with what was actually awarded.
     */
    private static void handleXpGained(@Nonnull PlayerGainXpEvent event) {
        var playerRef = event.getPlayerRef();
        var ref = playerRef.getReference();
        if (ref != null) Moments.count(ref, playerRef.getUuid(), GainXpQuestProgression.class, event.getSkillId(), null, event.getFinalAmount());
    }

    /**
     * Qualified by the skill whose tree holds the ability, the way MMO Skill Tree qualifies its own.
     */
    private static void handleAbilityCast(@Nonnull PlayerAbilityUsedEvent event) {
        String abilityId = event.spec.id();
        var placement = SkillTreeService.findAbilityPlacement(abilityId);

        Moments.count(event.playerEntityRef, event.playerRef.getUuid(), AbilityCastQuestProgression.class, abilityId, placement == null ? null : placement.skillId(), 1);
    }

    /**
     * A bounty is one of CommonLib's quests, counted once collected.
     */
    private static void handleQuestClaimed(@Nonnull QuestClaimedEvent event) {
        String questId = event.questId();
        if (!MmoBounties.isBounty(questId)) return;

        // ponytail: a bounty listed on several boards counts for its first board only
        String board = MmoBounties.primaryBoardOf(questId);
        if (board == null) return;

        String difficulty = MmoBounties.difficultyOf(questId);
        EntityComponents.update(event.playerId(), components -> {
            var ref = components.getReference();
            if (ref != null) Moments.count(ref, event.playerId(), CompleteBountyQuestProgression.class, board, difficulty.isEmpty() ? null : difficulty, 1);
        });
    }

    /**
     * Fails the load once per asset naming a skill MMO Skill Tree does not know.
     */
    private static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            String skill = asset instanceof MMOSkillTreeGainXpQuestAsset gainXp ? gainXp.getTarget()
                         : asset instanceof MMOSkillTreeAbilityCastQuestAsset cast ? cast.getQualifier()
                         : null;

            if (skill != null && !SkillRegistry.getInstance().isKnownSkill(skill)) {
                event.failed(true, "Quest asset '" + asset.getId() + "' names an unknown MMO Skill Tree skill '" + skill + "'");
            }
        }
    }
}
