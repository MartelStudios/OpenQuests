package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.services.QuestService;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestStore {
    public static final BuilderCodec<QuestStore> CODEC = BuilderCodec.builder(QuestStore.class, QuestStore::new)
                                                                     .append(new KeyedCodec<>("Quests", new SetCodec<>(Codec.UUID_BINARY, HashSet<UUID>::new, false)), (questStore, uuids) -> questStore.questIds.addAll(uuids), (questStore) -> questStore.questIds)
                                                                     .add()
                                                                     .build();

    private final Set<UUID> questIds = ConcurrentHashMap.newKeySet();

    private boolean loaded;

    public QuestStore() {

    }

    public QuestStore(QuestStore other) {
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

        return this.questIds.stream().map(QuestService.get()::getQuest).toList();
    }

    /**
     * @return {@code true} the first time this is called for this resource instance, {@code false}
     * afterward. Used to lazily load this world's quests into the {@link QuestDataStore} exactly once.
     */
    public boolean consumeNeedsLoad() {
        if (loaded) return false;
        loaded = true;
        return true;
    }

    public void loadAll() {
        if (!consumeNeedsLoad()) return;

        for (UUID questId : questIds) {
            QuestService.get().loadQuest(questId);
        }
    }

    @Nullable
    @Override
    public QuestStore clone() {
        return new QuestStore(this);
    }
}
