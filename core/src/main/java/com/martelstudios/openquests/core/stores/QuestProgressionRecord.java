package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

public class QuestProgressionRecord {

    public static final BuilderCodec<QuestProgressionRecord> CODEC = BuilderCodec.builder(QuestProgressionRecord.class, QuestProgressionRecord::new)
                                                                                 .append(new KeyedCodec<>("Quest", AbstractQuestProgression.CODEC), (record, quest) -> record.quest = quest, record -> record.quest)
                                                                                 .add()
                                                                                 .build();

    public AbstractQuestProgression<?> quest;

    protected QuestProgressionRecord() {
    }

    public QuestProgressionRecord(AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }
}
