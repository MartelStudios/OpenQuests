package com.martelstudios.hyquests.core.visitors;

import com.martelstudios.hyquests.core.models.AbstractQuestProgression;

public interface QuestVisitor<Q extends AbstractQuestProgression<Q>> {
    Class<Q> getQuestType();

    void progress(Q quest);
}
