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

    /** Quests this one matched but was not allowed to abandon, so a caller can say which it was. */
    private int refused;

    public QuestStateVisitor(@Nonnull UUID playerId, @Nonnull String questRef, @Nonnull QuestState state) {
        this.playerId = playerId;
        this.questRef = questRef;
        this.state = state;
    }

    @Override
    public void progress(AbstractQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (!questRef.equals(quest.getAssetId()) && !questRef.equals(quest.getId().toString())) return;

        // Refused here rather than only where the journal hides its button, so the rule holds
        // wherever a player gives up. Forcing another outcome stays an administrator matter.
        if (state == QuestState.ABANDONED && !quest.canBeAbandoned()) {
            refused++;
            return;
        }

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

    /**
     * @return how many it matched but was not allowed to abandon, so a caller can tell a quest it
     * could not find from one it may not give up.
     */
    public int getRefused() {
        return refused;
    }
}
