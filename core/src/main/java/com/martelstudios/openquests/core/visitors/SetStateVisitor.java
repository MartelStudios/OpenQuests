package com.martelstudios.openquests.core.visitors;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;

public class SetStateVisitor implements QuestVisitor<AbstractQuestProgression<?>> {
    private final QuestState state;

    public SetStateVisitor(@Nonnull QuestState state) {
        this.state = state;
    }

    @Override
    public void progress(AbstractQuestProgression<?> quest) {
        quest.setState(state).markDirty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<AbstractQuestProgression<?>> getQuestType() {
        return (Class<AbstractQuestProgression<?>>) (Class<?>) AbstractQuestProgression.class;
    }
}
