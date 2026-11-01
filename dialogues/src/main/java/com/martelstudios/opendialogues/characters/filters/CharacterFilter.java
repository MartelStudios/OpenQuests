package com.martelstudios.opendialogues.characters.filters;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.martelstudios.opendialogues.api.DialogueRegistry;

import javax.annotation.Nonnull;

/**
 * Whether a distribution may draw for an NPC where it stands. Each kind registers under a
 * {@code "Type"} tag through {@link DialogueRegistry#registerFilter}, so another mod can narrow it.
 */
public abstract class CharacterFilter {

    /**
     * Polymorphic dispatcher over every registered kind.
     */
    public static final CodecMapCodec<CharacterFilter> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<CharacterFilter> BASE_CODEC = BuilderCodec.abstractBuilder(CharacterFilter.class)
                                                                       .append(new KeyedCodec<>("Not", Codec.BOOLEAN), (filter, not) -> filter.not = not, filter -> filter.not)
                                                                       .add()
                                                                       .build();

    protected boolean not;

    /**
     * @return whether the filter holds for that NPC, {@code Not} applied.
     */
    public final boolean test(@Nonnull FilterContext context) {
        return evaluate(context) != not;
    }

    /**
     * @return whether the filter holds for that NPC, before {@code Not} inverts it.
     */
    protected abstract boolean evaluate(@Nonnull FilterContext context);
}
