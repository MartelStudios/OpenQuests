package com.martelstudios.openquests.extension.quests.loot.npc;

import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Makes on each loot step what the roll decided for it, so the change is announced and stored like
 * any other progress.
 */
public class LootNpcCreditVisitor implements QuestVisitor<LootNpcQuestProgression> {

    private final UUID killerId;
    private final Map<UUID, List<QuestOperation<? super LootNpcQuestProgression>>> operations;

    /**
     * @param operations what to make on each quest, by quest id, in order
     */
    public LootNpcCreditVisitor(@Nonnull UUID killerId, @Nonnull Map<UUID, List<QuestOperation<? super LootNpcQuestProgression>>> operations) {
        this.killerId = killerId;
        this.operations = operations;
    }

    @Override
    public void progress(LootNpcQuestProgression quest) {
        var forQuest = operations.get(quest.getId());
        if (forQuest == null) return;

        for (var operation : forQuest) quest.apply(operation);
    }

    @Override
    public Class<LootNpcQuestProgression> getQuestType() {
        return LootNpcQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return killerId;
    }
}
