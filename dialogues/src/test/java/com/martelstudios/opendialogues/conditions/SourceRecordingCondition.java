package com.martelstudios.opendialogues.conditions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.sessions.DialogueSource;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * A third-party condition that holds and keeps the source of every context it judges.
 */
public class SourceRecordingCondition extends DialogueCondition {

    public static final String TYPE = "TestRecordsSource";

    public static final BuilderCodec<SourceRecordingCondition> CODEC = BuilderCodec.builder(SourceRecordingCondition.class, SourceRecordingCondition::new, DialogueCondition.BASE_CODEC).build();

    public static final List<DialogueSource> SEEN = new ArrayList<>();

    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        SEEN.add(context.source());
        return true;
    }
}
