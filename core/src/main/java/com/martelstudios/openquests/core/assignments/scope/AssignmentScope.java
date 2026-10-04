package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;
import com.martelstudios.openquests.core.assignments.trigger.AssignmentTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Who an assignment hands its quests to, given an occasion: the player it concerns, a world, the
 * server. Each kind finds its holders, reaches each on the thread allowed to touch it, and says
 * what tells one hand-out to a holder from the next.
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
     * Offers the assignment's quests to every holder this occasion concerns, through
     * {@link com.martelstudios.openquests.core.assignments.QuestAssignmentService#offerAll}.
     */
    public abstract void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion);

    /**
     * Called on every world entry, whatever the trigger: a scope sharing quests between worlds
     * hands the entering player those already running in the world they arrive in.
     */
    public void onEnterWorld(@Nonnull OpenQuestAssignment assignment, @Nonnull UUID playerId, @Nonnull World world) {}

    /**
     * @return what is wrong with this scope under that trigger, {@code null} when nothing is.
     */
    @Nullable
    public String findInconsistency(@Nonnull AssignmentTrigger trigger) {
        return null;
    }
}
