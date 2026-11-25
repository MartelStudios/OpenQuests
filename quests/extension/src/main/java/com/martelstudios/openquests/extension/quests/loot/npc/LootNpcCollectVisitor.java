package com.martelstudios.openquests.extension.quests.loot.npc;

import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Finds the killer's running loot steps the victim may drop for, changing nothing. Going through a
 * visitor puts each step to its asset's constraints first, as any counted action is.
 */
public class LootNpcCollectVisitor implements QuestVisitor<LootNpcQuestProgression> {

    /**
     * A step wanting the item, with the source that applies to the victim.
     */
    public record Found(@Nonnull LootNpcQuestProgression quest, int entry) {}

    private final UUID killerId;
    private final NPCEntity victim;
    private final List<Found> found = new ArrayList<>();

    public LootNpcCollectVisitor(@Nonnull UUID killerId, @Nonnull NPCEntity victim) {
        this.killerId = killerId;
        this.victim = victim;
    }

    @Override
    public void progress(LootNpcQuestProgression quest) {
        if (!quest.getPlayers().contains(killerId)) return;
        if (quest.isOver() || quest.getAsset() == null) return;

        int entry = quest.matchingEntry(victim);
        if (entry >= 0) found.add(new Found(quest, entry));
    }

    /**
     * @return the steps found, in the order the killer's quests were visited.
     */
    @Nonnull
    public List<Found> getFound() {
        return found;
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
