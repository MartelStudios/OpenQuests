package com.martelstudios.openquests.core.persistence.disk;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A scope's set of quest ids, as one file. Its own type rather than a bare array because a
 * {@code DataStore} writes documents, and because a scope will want a field or two of its own
 * before long.
 */
public class QuestIndexRecord {

    public static final BuilderCodec<QuestIndexRecord> CODEC = BuilderCodec.builder(QuestIndexRecord.class, QuestIndexRecord::new)
                                                                           .append(new KeyedCodec<>("Quests", new SetCodec<>(Codec.UUID_STRING, HashSet<UUID>::new, false)), (record, ids) -> record.questIds.addAll(ids), record -> record.questIds)
                                                                           .add()
                                                                           .build();

    private final Set<UUID> questIds = new HashSet<>();

    public QuestIndexRecord() {}

    public QuestIndexRecord(@Nonnull Set<UUID> questIds) {
        this.questIds.addAll(questIds);
    }

    @Nonnull
    public Set<UUID> getQuestIds() {
        return questIds;
    }
}
