package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.teams.TeamChange;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** Persists inherited quests and history identically inside a transaction and during recovery. */
public final class TeamChangePersistence {
    private TeamChangePersistence() {}

    /** Restores missing copies only; retries retain newer progress and assignment history. */
    @Nonnull
    public static List<AbstractQuestProgression<?>> restore(@Nonnull QuestStorage storage, @Nonnull TeamChange change) {
        if (change.to() == null) return List.of();
        String holder = "team:" + change.to();
        List<AbstractQuestProgression<?>> copies = new ArrayList<>();
        for (QuestProgressionRecord snapshot : change.copies()) {
            AbstractQuestProgression<?> actual = storage.loadProgression(snapshot.quest.getId());
            if (actual == null) {
                var moved = storage.writeProgressions(List.of(snapshot.quest));
                actual = moved.isEmpty() ? snapshot.quest : storage.loadProgression(snapshot.quest.getId());
                if (actual == null) throw new QuestStorageException("Could not restore team quest " + snapshot.quest.getId());
            }
            copies.add(actual);
            if (actual.getParentId() == null && !actual.isOver()) storage.addToIndex(holder, List.of(actual.getId()));
        }
        change.assignments().forEach((assignment, records) -> records.forEach((asset, record) ->
                storage.claimAssignment(holder, assignment, asset, null, record)));
        return copies;
    }
}
