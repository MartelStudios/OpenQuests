package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestGiver;

import javax.annotation.Nullable;

/**
 * A quest off any store, its parent handed in since no store is there to look it up.
 */
public final class TestQuest extends AbstractQuestProgression<TestQuest> {

    static final BuilderCodec<TestQuest> CODEC = BuilderCodec.builder(TestQuest.class, TestQuest::new, AbstractQuestProgression.BASE_CODEC).build();

    @Nullable
    private AbstractCompositeQuestProgression<?> parent;

    /**
     * @return the quest built the way a giver hands it out, under asset {@code Q}.
     */
    public static <Q extends AbstractQuestProgression<Q>> Q givenBy(Q quest, @Nullable QuestGiver giver) {
        return AbstractQuestProgression.Builder.of(quest, "Q").giver(giver).build();
    }

    TestQuest stepOf(AbstractCompositeQuestProgression<?> parent) {
        this.parent = parent;
        return this;
    }

    @Nullable
    @Override
    public AbstractCompositeQuestProgression<?> getParent() {
        return parent;
    }

    /**
     * The smallest composite there is.
     */
    static final class Group extends AbstractCompositeQuestProgression<Group> {
        static final BuilderCodec<Group> CODEC = BuilderCodec.builder(Group.class, Group::new, AbstractCompositeQuestProgression.BASE_CODEC).build();
    }
}
