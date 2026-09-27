package com.martelstudios.openquests.extension.constraints.time;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Quests that end in time: a limit counted from their start, or a fixed moment. The core watches
 * the deadlines; these only say when.
 */
public final class TimeConstraintsFeature {
    public static final String TIME_LIMIT_TYPE_ID = "TimeLimit";
    public static final String DEADLINE_TYPE_ID = "Deadline";

    private TimeConstraintsFeature() {}

    /**
     * Registers both constraints under their type ids, and how the journal describes them.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestConstraint.CODEC.register(TIME_LIMIT_TYPE_ID, TimeLimitConstraint.class, TimeLimitConstraint.CODEC);
        QuestConstraint.CODEC.register(DEADLINE_TYPE_ID, DeadlineConstraint.class, DeadlineConstraint.CODEC);

        QuestPageService.register(new TimeLimitRenderer());
        QuestPageService.register(new DeadlineRenderer());
    }
}
