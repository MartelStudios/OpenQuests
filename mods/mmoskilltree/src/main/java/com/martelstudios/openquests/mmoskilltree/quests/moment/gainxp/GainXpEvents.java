package com.martelstudios.openquests.mmoskilltree.quests.moment.gainxp;

import com.martelstudios.openquests.mmoskilltree.quests.moment.Moments;
import com.ziggfreed.mmoskilltree.api.events.PlayerGainXpEvent;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Counts XP for {@link GainXpQuestProgression}s as MMO Skill Tree awards it.
 */
public final class GainXpEvents {

    /**
     * Held so the very listener added can be removed: MMO Skill Tree keeps its listeners by identity.
     */
    public static final Consumer<PlayerGainXpEvent> XP_LISTENER = GainXpEvents::handleXpGained;

    private GainXpEvents() {}

    /**
     * Fired as the XP is awarded, on the player's world thread, with what was actually awarded.
     */
    private static void handleXpGained(@Nonnull PlayerGainXpEvent event) {
        var playerRef = event.getPlayerRef();
        var ref = playerRef.getReference();
        if (ref != null) Moments.count(ref, playerRef.getUuid(), GainXpQuestProgression.class, event.getSkillId(), null, event.getFinalAmount());
    }
}
