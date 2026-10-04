package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * A holder many players share, a world, a group of worlds or the server, whose records live in a
 * store of their own, kept in memory and written conditionally: servers sharing one never both
 * hand the same occasion out.
 */
public final class SharedAssignmentHolder implements AssignmentHolder {

    @Nonnull
    private final SharedAssignmentRecords records;

    @Nonnull
    private final String key;

    @Nonnull
    private final QuestScope scope;

    @Nullable
    private final UUID joining;

    /**
     * The quests its index names, read once per offer: each one would otherwise be read back again
     * for every quest the assignment lists. Dropped once a hand-out changes them.
     */
    @Nullable
    private List<AbstractQuestProgression<?>> quests;

    /**
     * @param key the holder's key in the store, the same as its index: {@code world:<uuid>},
     * {@code worlds:<group>}, {@code universe}
     * @param scope the scope a quest handed out here is shared through, which indexes its quests
     * @param joining the player whose arrival this is, not among those the scope reaches yet
     */
    public SharedAssignmentHolder(@Nonnull SharedAssignmentRecords records, @Nonnull String key, @Nonnull QuestScope scope, @Nullable UUID joining) {
        this.records = records;
        this.key = key;
        this.scope = scope;
        this.joining = joining;
    }

    @Nonnull
    @Override
    public String getKey() {
        return key;
    }

    @Nullable
    @Override
    public AssignmentRecord getRecord(@Nonnull String assignmentId, @Nonnull String questAssetId) {
        return records.get(key).get(assignmentId, questAssetId);
    }

    @Nonnull
    @Override
    public Collection<AbstractQuestProgression<?>> getQuests() {
        if (quests != null) return quests;

        List<AbstractQuestProgression<?>> read = new ArrayList<>();
        for (UUID questId : new ArrayList<>(scope.getQuestIds())) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
            if (quest != null) read.add(quest);
        }
        quests = read;
        return read;
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
     * Claimed first: a quest is only created by the server whose write went through. Shared the
     * way any quest of that scope is, a chain's follow-ups included.
     */
    @Override
    public boolean handOut(@Nonnull AbstractQuestProgression<?> quest, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next) {
        if (!records.claim(key, assignmentId, questAssetId, expected, next)) return false;

        quests = null;
        QuestProgressionService.get().registerQuest(quest);
        scope.share(quest);
        if (joining != null) QuestProgressionService.get().joinQuest(quest, joining);
        return true;
    }

    /**
     * A claim is only ever lost to another server writing first, and losing one read the records
     * again already: the quests may have changed with them.
     */
    @Override
    public boolean refresh() {
        quests = null;
        return true;
    }
}
