package com.martelstudios.openquests.core.visitors;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Update the state of the quests a player holds from one reference.
 */
public class PlayerSetStateVisitor implements QuestVisitor<AbstractQuestProgression<?>> {
    private final UUID playerId;

    /**
     * A quest id or an asset id: the second matches every quest the player holds from it.
     */
    private final String questRef;

    private final QuestState state;

    private int matched;

    /**
     * Quests this one matched but was not allowed to update.
     */
    private int refused;

    public PlayerSetStateVisitor(@Nonnull UUID playerId, @Nonnull String questRef, @Nonnull QuestState state) {
        this.playerId = playerId;
        this.questRef = questRef;
        this.state = state;
    }

    @Override
    public void progress(AbstractQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (!questRef.equals(quest.getAssetId()) && !questRef.equals(quest.getId().toString())) return;

        switch (state) {
            case ABANDONED -> {
                if (quest.canBeAbandoned()) {
                    quest.abandonPlayer(playerId);
                    matched++;
                } else {
                    refused++;
                }
            }
            default -> {
                quest.setState(state).markDirty();
                matched++;
            }
        }
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
     * @return how many it matched but was not allowed update.
     */
    public int getRefused() {
        return refused;
    }
}
