package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.models.AbstractQuest;

public class QuestRecord {

    public static final BuilderCodec<QuestRecord> CODEC = BuilderCodec.builder(QuestRecord.class, QuestRecord::new)
                                                                      .append(new KeyedCodec<>("Quest", AbstractQuest.CODEC), (record, quest) -> record.quest = quest, record -> record.quest)
                                                                      .add()
                                                                      .build();

    public AbstractQuest<?> quest;

    protected QuestRecord() {
    }

    public QuestRecord(AbstractQuest<?> quest) {
        this.quest = quest;
    }
}
