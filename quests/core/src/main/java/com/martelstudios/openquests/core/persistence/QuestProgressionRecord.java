package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * One progression, as it is written down. The wrapper exists because a progression is polymorphic
 * and a document needs a root that is not, and it is shared by every backend so a quest written by
 * one can be read by another.
 */
public class QuestProgressionRecord {

    public static final BuilderCodec<QuestProgressionRecord> CODEC = BuilderCodec.builder(QuestProgressionRecord.class, QuestProgressionRecord::new)
                                                                                 .append(new KeyedCodec<>("Quest", AbstractQuestProgression.CODEC), (record, quest) -> record.quest = quest, record -> record.quest)
                                                                                 .documentation("Quest instance and its saved progress.")
                                                                                 .add()
                                                                                 .documentation("Wraps a quest instance for storage.")
                                                                                 .build();

    public AbstractQuestProgression<?> quest;

    protected QuestProgressionRecord() {
    }

    public QuestProgressionRecord(AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }
}
