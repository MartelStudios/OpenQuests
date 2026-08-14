package com.martelstudios.hyquests.core.visitors;

import com.martelstudios.hyquests.core.models.AbstractQuest;

public interface QuestVisitor<Q extends AbstractQuest<Q>> {
    Class<Q> getQuestType();

    void progress(Q quest);
}
