package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Settles a {@link CompositeQuestProgression} from its children's outcomes.
 */
public class CompositeQuestVisitor implements QuestVisitor<CompositeQuestProgression> {

    @Nonnull
    private final UUID childId;

    /**
     * The state the child moved to, rather than whatever it carries by the time this runs.
     */
    @Nonnull
    private final QuestState outcome;

    /**
     * How many changes of state the child had been through, which orders this news among the rest.
     */
    private final int transitions;

    public CompositeQuestVisitor(@Nonnull UUID childId, @Nonnull QuestState outcome, int transitions) {
        this.childId = childId;
        this.outcome = outcome;
        this.transitions = transitions;
    }

    @Override
    public void progress(CompositeQuestProgression quest) {
        quest.apply(new CompositeQuestProgression.Hear(List.of(new CompositeQuestProgression.Heard(childId, outcome, transitions))));
        quest.apply(new CompositeQuestProgression.Weigh());
    }

    @Override
    public Class<CompositeQuestProgression> getQuestType() {
        return CompositeQuestProgression.class;
    }
}
