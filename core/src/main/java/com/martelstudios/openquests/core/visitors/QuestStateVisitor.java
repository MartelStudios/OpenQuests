package com.martelstudios.openquests.core.visitors;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Forces the state of the quests a player holds from one reference. Visits every type, since what
 * it writes is the state each of them already carries.
 */
public class QuestStateVisitor implements QuestVisitor<AbstractQuestProgression<?>> {

    private final UUID playerId;

    /** A quest id or an asset id: the second matches every quest the player holds from it. */
    private final String questRef;

    private final QuestState state;

    private int matched;

    public QuestStateVisitor(@Nonnull UUID playerId, @Nonnull String questRef, @Nonnull QuestState state) {
        this.playerId = playerId;
        this.questRef = questRef;
        this.state = state;
    }

    @Override
    public void progress(AbstractQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (!questRef.equals(quest.getAssetId()) && !questRef.equals(quest.getId().toString())) return;

        quest.setState(state).markDirty();
        matched++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<AbstractQuestProgression<?>> getQuestType() {
        return (Class<AbstractQuestProgression<?>>) (Class<?>) AbstractQuestProgression.class;
    }

    /**
     * @return how many quests this visitor wrote to, known only once it has run.
     */
    public int getMatched() {
        return matched;
    }
}
