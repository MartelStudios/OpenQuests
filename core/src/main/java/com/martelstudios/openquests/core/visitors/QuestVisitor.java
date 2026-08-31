package com.martelstudios.openquests.core.visitors;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * Carries the context of an event to the quests it can progress. The bound is a wildcard rather
 * than the self-type of {@link AbstractQuestProgression}, so a visitor can target the base type
 * and reach every quest whatever its type.
 */
public interface QuestVisitor<Q extends AbstractQuestProgression<?>> {
    Class<Q> getQuestType();

    void progress(Q quest);
}
