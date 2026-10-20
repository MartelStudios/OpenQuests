package com.martelstudios.openquests.mmoskilltree.quests.moment.abilitycast;

import com.martelstudios.openquests.mmoskilltree.quests.moment.Moments;
import com.ziggfreed.mmoskilltree.ability.cast.PlayerAbilityUsedEvent;
import com.ziggfreed.mmoskilltree.skilltree.SkillTreeService;

import javax.annotation.Nonnull;

/**
 * Counts casts for {@link AbilityCastQuestProgression}s.
 */
public final class AbilityCastEvents {

    private final Moments moments;

    public AbilityCastEvents(@Nonnull Moments moments) {
        this.moments = moments;
    }

    /**
     * Qualified by the skill whose tree holds the ability, the way MMO Skill Tree qualifies its own.
     */
    public void handleAbilityCast(@Nonnull PlayerAbilityUsedEvent event) {
        String abilityId = event.spec.id();
        var placement = SkillTreeService.findAbilityPlacement(abilityId);

        moments.count(event.playerEntityRef, event.playerRef.getUuid(), AbilityCastQuestProgression.class, abilityId, placement == null ? null : placement.skillId(), 1);
    }
}
