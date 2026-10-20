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
    private final Consumer<PlayerGainXpEvent> xpListener = this::handleXpGained;

    private final Moments moments;

    public GainXpEvents(@Nonnull Moments moments) {
        this.moments = moments;
    }

    /**
     * @return the listener to add to MMO Skill Tree, and to take back by the same identity.
     */
    @Nonnull
    public Consumer<PlayerGainXpEvent> xpListener() {
        return xpListener;
    }

    /**
     * Fired as the XP is awarded, on the player's world thread, with what was actually awarded.
     */
    private void handleXpGained(@Nonnull PlayerGainXpEvent event) {
        var playerRef = event.getPlayerRef();
        var ref = playerRef.getReference();
        if (ref != null) moments.count(ref, playerRef.getUuid(), GainXpQuestProgression.class, event.getSkillId(), null, event.getFinalAmount());
    }
}
