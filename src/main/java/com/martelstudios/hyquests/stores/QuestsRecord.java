package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.services.QuestProgressionService;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestsRecord {
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

    public void register(UUID questId) {
        this.questIds.add(questId);
    }

    public void unregister(UUID questId) {
        this.questIds.remove(questId);
    }

    public boolean contains(UUID questId) {
        return this.questIds.contains(questId);
    }

    /**
     * @return the live set of registered quest ids.
     */
    public Set<UUID> getAllIds() {
        return this.questIds;
    }

    public List<? extends AbstractQuest<?>> getAllQuests() {
        if (!loaded) loadAll();

        return this.questIds.stream().map(QuestProgressionService.get()::getQuest).toList();
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

        for (UUID questId : questIds) {
            QuestProgressionService.get().loadQuest(questId);
        }
    }

    @Nullable
    @Override
    public QuestsRecord clone() {
        return new QuestsRecord(this);
    }
}
