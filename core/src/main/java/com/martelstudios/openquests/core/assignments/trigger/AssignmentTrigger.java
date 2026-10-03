package com.martelstudios.openquests.core.assignments.trigger;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * When an assignment hands its quests out: the occasions it reacts to, each with where and when it
 * happens. Who receives the quest is the scope's to say, and whether it is handed out again the
 * repeat's.
 */
public abstract class AssignmentTrigger {

    /**
     * Polymorphic dispatcher: each kind registers under a {@code "Type"} tag, so a plugin can add
     * its own.
     */
    public static final CodecMapCodec<AssignmentTrigger> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<AssignmentTrigger> BASE_CODEC = BuilderCodec.abstractBuilder(AssignmentTrigger.class).build();

    /**
     * @param player the connecting player's holder: they are not online yet
     * @return the occasion that connection is for this trigger, {@code null} when it does not react.
     */
    @Nullable
    public Occasion onConnect(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
        return null;
    }

    /**
     * @param player the entering player's holder: they are not in the world's store yet
     * @return the occasion that entry is for this trigger, {@code null} when it does not react.
     */
    @Nullable
    public Occasion onEnterWorld(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull World world) {
        return null;
    }

    /**
     * Asked every so often, whoever is there: the way a trigger keeping time hands out a period
     * as it begins, and keeps handing it to holders reached while it lasts.
     *
     * @return the occasion it is at that moment for nobody in particular, {@code null} when it
     * keeps no time.
     */
    @Nullable
    public Occasion onTick(@Nonnull Instant now) {
        return null;
    }

    /**
     * @return whether its occasions happen in a world, which a world scope naming no worlds of
     * its own takes as the one to share the quest in.
     */
    public boolean hasPlace() {
        return false;
    }

    /**
     * @return what is wrong with this trigger, {@code null} when nothing is, so the assignment is
     * refused as it loads rather than doing nothing in game.
     */
    @Nullable
    public String findInconsistency() {
        return null;
    }
}
