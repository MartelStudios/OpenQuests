package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestsRecord {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<QuestsRecord> CODEC = BuilderCodec.builder(QuestsRecord.class, QuestsRecord::new)
                                                                       .append(new KeyedCodec<>("Quests", new SetCodec<>(Codec.UUID_BINARY, HashSet<UUID>::new, false)), (questSet, uuids) -> questSet.questIds.addAll(uuids), (questSet) -> questSet.questIds)
                                                                       .add()
                                                                       .build();

    private final Set<UUID> questIds = ConcurrentHashMap.newKeySet();

    private boolean loaded;

    public QuestsRecord() {

    }

    public QuestsRecord(QuestsRecord other) {
        this.questIds.addAll(other.questIds);
        this.loaded = other.loaded;
    }

    /**
     * @return {@code true} if the quest was not already registered.
     */
    public boolean register(UUID questId) {
        return this.questIds.add(questId);
    }

    public boolean unregister(UUID questId) {
        return this.questIds.remove(questId);
    }

    public boolean contains(UUID questId) {
        return this.questIds.contains(questId);
    }

    /**
     * Resolves every registered id, loading them first if needed.
     *
     * @return the quests of this scope, without the ids that no longer resolve.
     */
    public List<AbstractQuestProgression<?>> getAllQuests() {
        loadAll();

        List<AbstractQuestProgression<?>> quests = new ArrayList<>(questIds.size());
        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest != null) quests.add(quest);
        }
        return quests;
    }

    /**
     * @return the live set of registered quest ids.
     */
    public Set<UUID> getAllIds() {
        return this.questIds;
    }

    /**
     * @return {@code true} the first time this is called for this resource instance, {@code false}
     * afterward. Used to lazily load this world's quests into the {@link QuestProgressionStore} exactly once.
     */
    public boolean consumeNeedsLoad() {
        if (loaded) return false;
        loaded = true;
        return true;
    }

    public void loadAll() {
        if (!consumeNeedsLoad()) return;

        for (UUID questId : new ArrayList<>(questIds)) {
            var quest = QuestProgressionService.get().loadQuest(questId);
            if (quest == null) {
                questIds.remove(questId);
                LOGGER.atWarning().log("Obsolete quest id %s, removed from store.", questId);
            }
        }
    }

    @Nullable
    @Override
    public QuestsRecord clone() {
        return new QuestsRecord(this);
    }
}
