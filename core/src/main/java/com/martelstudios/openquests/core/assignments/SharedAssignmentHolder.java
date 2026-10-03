package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A holder many players share, a world, a group of worlds or the server, whose records live in a
 * store of their own and are written conditionally: servers sharing one never both hand the same
 * occasion out.
 */
public final class SharedAssignmentHolder implements AssignmentHolder {

    @Nonnull
    private final QuestStorage storage;

    @Nonnull
    private final String key;

    @Nonnull
    private final Supplier<Collection<UUID>> questIds;

    @Nonnull
    private final Consumer<AbstractQuestProgression<?>> share;

    @Nullable
    private AssignmentRecords records;

    /**
     * @param key the holder's key in the store, the same as its index: {@code world:<uuid>},
     * {@code universe}
     * @param questIds the ids its index holds
     * @param share what makes a registered quest the holder's, and reach whoever it is for
     */
    public SharedAssignmentHolder(@Nonnull QuestStorage storage, @Nonnull String key, @Nonnull Supplier<Collection<UUID>> questIds, @Nonnull Consumer<AbstractQuestProgression<?>> share) {
        this.storage = storage;
        this.key = key;
        this.questIds = questIds;
        this.share = share;
    }

    @Nonnull
    @Override
    public String getKey() {
        return key;
    }

    @Nullable
    @Override
    public AssignmentRecord getRecord(@Nonnull String assignmentId, @Nonnull String questAssetId) {
        return records().get(assignmentId, questAssetId);
    }

    @Nonnull
    @Override
    public Collection<AbstractQuestProgression<?>> getQuests() {
        List<AbstractQuestProgression<?>> quests = new ArrayList<>();
        for (UUID questId : new ArrayList<>(questIds.get())) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
            if (quest != null) quests.add(quest);
        }
        return quests;
    }

    /**
     * Everyone gone and someone having given up is the end of it, even before the quest heard so.
     */
    @Override
    public boolean isRunning(@Nonnull AbstractQuestProgression<?> quest) {
        if (QuestProgressionService.get().getLiveQuest(quest.getId()) == null) return false;

        return !quest.getPlayers().isEmpty() || quest.getAbandonedPlayers().isEmpty();
    }

    /**
     * Claimed first: a quest is only created by the server whose write went through.
     */
    @Override
    public boolean handOut(@Nonnull AbstractQuestProgression<?> quest, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next) {
        if (!storage.claimAssignment(key, assignmentId, questAssetId, expected, next)) return false;

        records().put(assignmentId, questAssetId, next);
        QuestProgressionService.get().registerQuest(quest);
        share.accept(quest);
        return true;
    }

    /**
     * Read once per holder and occasion, the decision being made on what was read.
     */
    @Nonnull
    private AssignmentRecords records() {
        if (records == null) records = storage.loadAssignments(key);
        return records;
    }
}
