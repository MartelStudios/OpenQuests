package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.codec.ExtraInfo;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Copies a running quest for one player, so a player leaving a team keeps the progress made and
 * goes on alone from there. The source is never changed.
 */
public class QuestCopier {

    private final QuestProgressionService progression;

    public QuestCopier(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    /**
     * @return a registered copy under a new id held by the player alone, steps included, with the
     * progress, origin and start of the source. Its scope is left unset for the caller to share.
     */
    @Nonnull
    public AbstractQuestProgression<?> copyFor(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        List<AbstractQuestProgression<?>> copies = snapshot(quest, playerId);
        // Registration precedes the membership event used to populate the online player's index.
        for (AbstractQuestProgression<?> item : copies) item.prepareCopy(item.getId(), item.getParentId(), null);
        copies.forEach(progression::registerLoadedQuest);
        AbstractQuestProgression<?> copy = copies.getLast();
        copy.addPlayer(playerId);
        return copy;
    }

    /** @return detached copies, children first, whose fixed ids can safely be persisted in a retried move. */
    @Nonnull
    public List<AbstractQuestProgression<?>> snapshot(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        List<AbstractQuestProgression<?>> copies = new ArrayList<>();
        copy(quest, null, playerId, copies);
        return copies;
    }

    /**
     * Steps are registered before their group, which reads them back as it enters the store. Not
     * through registration of a new quest, which would start the group with fresh steps.
     */
    @Nonnull
    private AbstractQuestProgression<?> copy(@Nonnull AbstractQuestProgression<?> quest, @Nullable UUID parentId,
                                              UUID playerId, List<AbstractQuestProgression<?>> copies) {
        AbstractQuestProgression<?> copy;
        synchronized (quest) {
            var info = ExtraInfo.THREAD_LOCAL.get();
            copy = AbstractQuestProgression.CODEC.decode(AbstractQuestProgression.CODEC.encode(quest, info), info);
        }
        if (copy == null) throw new IllegalStateException("Cannot copy quest " + quest.getId());
        copy.prepareCopy(UUID.randomUUID(), parentId, playerId);
        if (quest instanceof AbstractCompositeQuestProgression<?> group && copy instanceof AbstractCompositeQuestProgression<?> copiedGroup) {
            List<UUID> childIds = new ArrayList<>();
            for (UUID childId : group.getChildIds()) {
                AbstractQuestProgression<?> child = progression.loadQuest(childId);
                if (child == null) throw new IllegalStateException("Cannot copy missing quest step " + childId);
                childIds.add(copy(child, copy.getId(), playerId, copies).getId());
            }
            copiedGroup.prepareCopiedChildren(childIds.toArray(UUID[]::new));
        }
        copies.add(copy);
        return copy;
    }
}
