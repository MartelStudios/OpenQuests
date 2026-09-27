package com.martelstudios.openquests.extension.constraints.death;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.constraints.QuestConstraint;

/**
 * Fails the quest the moment one of its players dies. A shared quest fails for everyone holding
 * it: the group was meant to get through together.
 */
public class FailOnDeathConstraint extends QuestConstraint {

    public static final BuilderCodec<FailOnDeathConstraint> CODEC = BuilderCodec.builder(FailOnDeathConstraint.class, FailOnDeathConstraint::new, QuestConstraint.BASE_CODEC)
                                                                                .build();

    protected FailOnDeathConstraint() {}
}
