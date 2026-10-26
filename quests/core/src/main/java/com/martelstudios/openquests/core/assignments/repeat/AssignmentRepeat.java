package com.martelstudios.openquests.core.assignments.repeat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.martelstudios.openquests.core.models.AssignmentRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Whether an occasion hands a holder the quest again. The trigger says when occasions come, the
 * scope who they are for; this only weighs what was handed before against what is still running.
 */
public abstract class AssignmentRepeat {

    /**
     * Polymorphic dispatcher: each kind registers under a {@code "Type"} tag, so a plugin can add
     * its own.
     */
    public static final CodecMapCodec<AssignmentRepeat> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<AssignmentRepeat> BASE_CODEC = BuilderCodec.abstractBuilder(AssignmentRepeat.class)
                                                                                .append(new KeyedCodec<>("Max", Codec.INTEGER), (repeat, max) -> repeat.max = max, repeat -> repeat.max == 0 ? null : Integer.valueOf(repeat.max))
                                                                                .add()
                                                                                .build();

    /**
     * How many times a holder may be handed the quest by this assignment, {@code 0} for no limit.
     */
    protected int max;

    /**
     * The cap is the same for every kind and weighed first: a holder handed the quest that many
     * times is never handed it again.
     */
    @Nonnull
    public final Decision decide(@Nonnull AssignmentHistory history) {
        AssignmentRecord record = history.getRecord();
        if (max > 0 && record != null && record.getCount() >= max) return Decision.SKIP;

        return decideWithin(history);
    }

    /**
     * @return how many times a holder may be handed the quest, {@code 0} for no limit.
     */
    public int getMax() {
        return max;
    }

    /**
     * @return what is wrong with this repeat, {@code null} when nothing is.
     */
    @Nullable
    public String findInconsistency() {
        return max < 0 ? "Max cannot be negative" : null;
    }

    @Nonnull
    protected abstract Decision decideWithin(@Nonnull AssignmentHistory history);

    /**
     * What an occasion comes to for one holder.
     */
    public enum Decision {
        /**
         * Nothing is handed out.
         */
        SKIP,

        /**
         * A new quest is handed out.
         */
        HAND_OUT,

        /**
         * A new quest is handed out, and what the earlier one opened, still running, fails.
         */
        REPLACE
    }
}
