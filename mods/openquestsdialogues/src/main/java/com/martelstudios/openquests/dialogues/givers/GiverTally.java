package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.SourceScope;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestTally;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Where a player stands with the quests from an asset handed out by the conversation's source,
 * its character or the NPC alone, read by the quest conditions' {@code GivenBy}.
 */
public final class GiverTally {

    private GiverTally() {}

    /**
     * Read on the player's world thread; elsewhere nothing can be read and the tally is empty.
     *
     * @param scope {@code Character} or {@code Npc}; {@code Any} is the player's whole tally, read from core instead.
     */
    @Nonnull
    public static QuestTally read(@Nonnull SourceScope scope, @Nonnull ConditionContext context, @Nonnull String assetId) {
        ComponentType<EntityStore, GiverQuestRecord> type = GiverQuestRecorder.type();
        if (context.player() == null || type == null || !context.canReadEntity()) return QuestTally.NONE;

        GiverQuestRecord record = context.store().getComponent(context.entity(), type);
        Collection<AbstractQuestProgression<?>> live = OpenQuestsServices.installed().getQuestProgressionStore().getLiveForAsset(assetId);
        return of(scope, context.source(), context.player().getUuid(), assetId, record != null ? record : new GiverQuestRecord(), live);
    }

    /**
     * @return the tally of the quests given by the scope's character or NPC: running ones from
     * {@code live}, ended ones from the record. {@link QuestTally#NONE} without a source to scope on.
     */
    @Nonnull
    public static QuestTally of(@Nonnull SourceScope scope, @Nullable DialogueSource source, @Nonnull UUID playerId, @Nonnull String assetId, @Nonnull GiverQuestRecord record, @Nonnull Collection<? extends AbstractQuestProgression<?>> live) {
        String character = scope.character(source);
        if (character == null) return QuestTally.NONE;

        UUID npc = scope.npc(source);
        List<? extends AbstractQuestProgression<?>> given = live.stream().filter(quest -> {
            Giver giver = Giver.of(quest);
            return giver != null && giver.character().equals(character) && (npc == null || giver.npc().equals(npc));
        }).toList();
        return QuestTally.of(record.completions(character, npc, assetId), playerId, given);
    }
}
