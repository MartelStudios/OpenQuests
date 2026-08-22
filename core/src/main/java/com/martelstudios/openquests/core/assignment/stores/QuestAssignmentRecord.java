package com.martelstudios.openquests.core.assignment.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.assignment.models.QuestAssignment;

public class QuestAssignmentRecord {

    public static final BuilderCodec<QuestAssignmentRecord> CODEC = BuilderCodec.builder(QuestAssignmentRecord.class, QuestAssignmentRecord::new)
                                                                                .append(new KeyedCodec<>("Assignment", QuestAssignment.CODEC), (record, assignment) -> record.assignment = assignment, record -> record.assignment)
                                                                                .add()
                                                                                .build();

    public QuestAssignment assignment;

    protected QuestAssignmentRecord() {}

    public QuestAssignmentRecord(QuestAssignment assignment) {
        this.assignment = assignment;
    }
}
