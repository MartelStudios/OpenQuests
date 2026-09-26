package com.martelstudios.openquests.core.visitors;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Carries the context of an event to the quests it can progress. The bound is a wildcard rather
 * than the self-type of {@link AbstractQuestProgression}, so a visitor can target the base type
 * and reach every quest whatever its type.
 */
public interface QuestVisitor<Q extends AbstractQuestProgression<?>> {
    Class<Q> getQuestType();

    void progress(Q quest);

    /**
     * @return the player whose action this carries, or {@code null} for a change the system makes
     * on its own. Only a player's action is put to the constraints of the asset: a quest being
     * settled, failed or abandoned never is.
     */
    @Nullable
    default UUID getActorId() {
        return null;
    }
}
