package com.martelstudios.opendialogues.routers;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * How a router picks among its targets. Each kind registers under a {@code "Type"} tag through
 * {@link DialogueRegistry#registerSelector}, so another mod can pick its own way.
 */
public abstract class DialogueSelector {

    /**
     * Polymorphic dispatcher over every registered kind.
     */
    public static final CodecMapCodec<DialogueSelector> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<DialogueSelector> BASE_CODEC = BuilderCodec.abstractBuilder(DialogueSelector.class).build();

    /**
     * A target the resolution leads nowhere from counts as a route that does not hold: pick on.
     *
     * @return the dialogue picked, or {@code null} when no target leads to one.
     */
    @Nullable
    public abstract OpenDialogueAsset select(@Nonnull ConditionContext context, @Nonnull TargetResolver resolver);

    /**
     * @return every target the selector may pick, so loading can check where they lead.
     */
    @Nonnull
    public abstract List<OpenDialoguesTarget> targets();

    /**
     * @return every condition the selector reads, so loading can validate them.
     */
    @Nonnull
    public abstract List<DialogueCondition> conditions();
}
