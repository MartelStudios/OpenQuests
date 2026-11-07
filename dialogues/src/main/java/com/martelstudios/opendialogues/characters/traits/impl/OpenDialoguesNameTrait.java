package com.martelstudios.opendialogues.characters.traits.impl;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.OpenDialogueTextListAsset;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Gives the NPC a name drawn once from a list, shown instead of its character's fixed one.
 */
public class OpenDialoguesNameTrait extends CharacterTrait {

    public static final String TYPE = "Name";

    public static final BuilderCodec<OpenDialoguesNameTrait> CODEC = BuilderCodec.builder(OpenDialoguesNameTrait.class, OpenDialoguesNameTrait::new, CharacterTrait.BASE_CODEC)
                                                                                 .append(new KeyedCodec<>("Names", OpenDialogueTextListAsset.CONTAINED_CODEC), (trait, names) -> trait.names = names, trait -> trait.names)
                                                                                 .add()
                                                                                 .build();

    @Nullable
    private String names;

    protected OpenDialoguesNameTrait() {}

    /**
     * @return the id of the text list drawn from.
     */
    @Nullable
    public String getNames() {
        return names;
    }

    @Override
    public void onAssigned(@Nonnull CharacterContext context) {
        OpenDialogueTextListAsset list = names == null ? null : context.assets().textList(names);
        if (list == null || list.getTexts().length == 0) return;
        context.identity().setName(list.getTexts()[context.random().nextInt(list.getTexts().length)]);
    }

    @Override
    public void validate(@Nonnull CharacterAssets assets, @Nonnull Consumer<String> problems) {
        OpenDialogueTextListAsset list = names == null ? null : assets.textList(names);
        if (list == null || list.getTexts().length == 0) problems.accept("draws its name from an unknown or empty list '" + names + "'");
    }
}
