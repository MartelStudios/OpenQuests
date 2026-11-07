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
 * Gives the NPC an epithet drawn once from a list, shown instead of its character's fixed one.
 */
public class OpenDialoguesEpithetTrait extends CharacterTrait {

    public static final String TYPE = "Epithet";

    public static final BuilderCodec<OpenDialoguesEpithetTrait> CODEC = BuilderCodec.builder(OpenDialoguesEpithetTrait.class, OpenDialoguesEpithetTrait::new, CharacterTrait.BASE_CODEC)
                                                                                    .append(new KeyedCodec<>("Epithets", OpenDialogueTextListAsset.CONTAINED_CODEC), (trait, epithets) -> trait.epithets = epithets, trait -> trait.epithets)
                                                                                    .add()
                                                                                    .build();

    @Nullable
    private String epithets;

    protected OpenDialoguesEpithetTrait() {}

    /**
     * @return the id of the text list drawn from.
     */
    @Nullable
    public String getEpithets() {
        return epithets;
    }

    @Override
    public void onAssigned(@Nonnull CharacterContext context) {
        OpenDialogueTextListAsset list = epithets == null ? null : context.assets().textList(epithets);
        if (list == null || list.getTexts().length == 0) return;
        context.identity().setEpithet(list.getTexts()[context.random().nextInt(list.getTexts().length)]);
    }

    @Override
    public void validate(@Nonnull CharacterAssets assets, @Nonnull Consumer<String> problems) {
        OpenDialogueTextListAsset list = epithets == null ? null : assets.textList(epithets);
        if (list == null || list.getTexts().length == 0) problems.accept("draws its epithet from an unknown or empty list '" + epithets + "'");
    }
}
