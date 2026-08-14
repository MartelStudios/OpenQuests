package com.martelstudios.hyquests.core.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuest;

public class QuestProgressionRecord {

    public static final BuilderCodec<QuestProgressionRecord> CODEC = BuilderCodec.builder(QuestProgressionRecord.class, QuestProgressionRecord::new)
                                                                                 .append(new KeyedCodec<>("Quest", AbstractQuest.CODEC), (record, quest) -> record.quest = quest, record -> record.quest)
                                                                                 .add()
                                                                                 .build();

    public AbstractQuest<?> quest;

    protected QuestProgressionRecord() {
    }

    public QuestProgressionRecord(AbstractQuest<?> quest) {
        this.quest = quest;
    }
}
