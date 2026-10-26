package com.martelstudios.opendialogues.conditions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Whether a route applies to a player. Each kind registers under a {@code "Type"} tag through
 * {@link DialogueRegistry#registerCondition}, so another mod can judge on what it knows.
 */
public abstract class DialogueCondition {

    /**
     * Polymorphic dispatcher over every registered kind.
     */
    public static final CodecMapCodec<DialogueCondition> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<DialogueCondition> BASE_CODEC = BuilderCodec.abstractBuilder(DialogueCondition.class)
                                                                         .append(new KeyedCodec<>("Not", Codec.BOOLEAN), (condition, not) -> condition.not = not, condition -> condition.not)
                                                                         .add()
                                                                         .build();

    protected boolean not;

    /**
     * @return whether the condition holds for that player now, {@code Not} applied.
     */
    public final boolean test(@Nonnull ConditionContext context) {
        return evaluate(context) != not;
    }

    /**
     * @return whether the condition holds for that player now, before {@code Not} inverts it.
     */
    protected abstract boolean evaluate(@Nonnull ConditionContext context);

    /**
     * Asked once every dialogue is loaded, so a wrong id shows at boot rather than in play.
     *
     * @param dialogues finds a loaded dialogue by id, {@code null} for none.
     * @return why the condition cannot work, {@code null} if it can.
     */
    @Nullable
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return null;
    }
}
