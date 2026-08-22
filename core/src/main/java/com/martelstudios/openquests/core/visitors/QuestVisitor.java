package com.martelstudios.openquests.core.visitors;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

public interface QuestVisitor<Q extends AbstractQuestProgression<Q>> {
    Class<Q> getQuestType();

    void progress(Q quest);
}
