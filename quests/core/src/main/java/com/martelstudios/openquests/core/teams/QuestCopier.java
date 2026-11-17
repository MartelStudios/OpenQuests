package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.codec.ExtraInfo;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;

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
        AbstractQuestProgression<?> copy = copy(quest, null);
        copy.addPlayer(playerId);
        return copy;
    }

    /**
     * Steps are registered before their group, which reads them back as it enters the store. Not
     * through registration of a new quest, which would start the group with fresh steps.
     */
    @Nonnull
    private AbstractQuestProgression<?> copy(@Nonnull AbstractQuestProgression<?> quest, @Nullable UUID parentId) {
        UUID copyId = UUID.randomUUID();
        BsonDocument document;
        synchronized (quest) {
            document = AbstractQuestProgression.CODEC.encode(quest, ExtraInfo.THREAD_LOCAL.get()).asDocument();
        }

        document.put("Id", new BsonString(copyId.toString()));
        document.put("Players", new BsonArray());
        document.remove("AbandonedPlayers");
        document.remove("Scope");
        if (parentId != null) document.put("ParentId", new BsonString(parentId.toString()));

        if (quest instanceof AbstractCompositeQuestProgression<?> group) {
            List<BsonString> childIds = new ArrayList<>();
            for (UUID childId : group.getChildIds()) {
                AbstractQuestProgression<?> child = progression.loadQuest(childId);
                if (child != null) childIds.add(new BsonString(copy(child, copyId).getId().toString()));
            }
            document.put("QuestIds", new BsonArray(childIds));
        }

        AbstractQuestProgression<?> copy = AbstractQuestProgression.CODEC.decode(document, ExtraInfo.THREAD_LOCAL.get());
        progression.registerLoadedQuest(copy);
        copy.markDirty();
        return copy;
    }
}
