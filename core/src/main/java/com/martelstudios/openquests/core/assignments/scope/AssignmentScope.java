package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.assignments.AssignmentTargets;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;
import com.martelstudios.openquests.core.assignments.trigger.AssignmentTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Who an assignment hands its quests to, given an occasion: the player it concerns, a world, a
 * group of worlds, the server. Each kind picks its holders among the targets it is given.
 */
public abstract class AssignmentScope {

    /**
     * Polymorphic dispatcher: each kind registers under a {@code "Type"} tag, so a plugin can add
     * its own.
     */
    public static final CodecMapCodec<AssignmentScope> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<AssignmentScope> BASE_CODEC = BuilderCodec.abstractBuilder(AssignmentScope.class).build();

    /**
     * Picks among the targets every holder this occasion concerns, each of which is then offered
     * the assignment's quests.
     */
    public abstract void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion, @Nonnull AssignmentTargets targets);

    /**
     * Asked on every world entry, whatever the trigger: a world of a group takes up the group's
     * running quests as it is entered, and the player entering joins them.
     *
     * @return the group of worlds this scope puts that world in, {@code null} for none.
     */
    @Nullable
    public String groupOf(@Nonnull OpenQuestAssignment assignment, @Nonnull World world) {
        return null;
    }

    /**
     * @return what is wrong with this scope under that trigger, {@code null} when nothing is.
     */
    @Nullable
    public String findInconsistency(@Nonnull AssignmentTrigger trigger) {
        return null;
    }
}
