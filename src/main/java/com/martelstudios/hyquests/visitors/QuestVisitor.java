package com.martelstudios.hyquests.visitors;

import com.martelstudios.hyquests.models.AbstractQuest;

public interface QuestVisitor<Q extends AbstractQuest<Q>> {
    Class<Q> getQuestType();

    void progress(Q quest);
}
