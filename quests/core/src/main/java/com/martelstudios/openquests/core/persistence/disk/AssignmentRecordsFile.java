package com.martelstudios.openquests.core.persistence.disk;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * What the assignments handed one shared holder, as one file: a {@code DataStore} writes documents,
 * not bare maps.
 */
public class AssignmentRecordsFile {

    public static final BuilderCodec<AssignmentRecordsFile> CODEC = BuilderCodec.builder(AssignmentRecordsFile.class, AssignmentRecordsFile::new)
                                                                                .append(new KeyedCodec<>("Assignments", AssignmentRecords.MAP_CODEC), (file, records) -> file.records.putAll(records), file -> file.records)
                                                                                .add()
                                                                                .build();

    private final Map<String, Map<String, AssignmentRecord>> records = new HashMap<>();

    public AssignmentRecordsFile() {}

    public AssignmentRecordsFile(@Nonnull Map<String, Map<String, AssignmentRecord>> records) {
        this.records.putAll(records);
    }

    /**
     * @return the records by assignment and then by quest.
     */
    @Nonnull
    public Map<String, Map<String, AssignmentRecord>> getRecords() {
        return records;
    }
}
