package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.QuestStateVersion;
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
     * The state the child moved to, rather than whatever it carries by the time this runs, with the
     * version ordering this news among the rest.
     */
    @Nonnull
    private final QuestStateVersion outcome;

    public CompositeQuestVisitor(@Nonnull UUID childId, @Nonnull QuestStateVersion outcome) {
        this.childId = childId;
        this.outcome = outcome;
    }

    @Override
    public void progress(CompositeQuestProgression quest) {
        quest.apply(new CompositeQuestProgression.Hear(List.of(new CompositeQuestProgression.Heard(childId, outcome))));
        quest.apply(new CompositeQuestProgression.Weigh());
    }

    @Override
    public Class<CompositeQuestProgression> getQuestType() {
        return CompositeQuestProgression.class;
    }
}
