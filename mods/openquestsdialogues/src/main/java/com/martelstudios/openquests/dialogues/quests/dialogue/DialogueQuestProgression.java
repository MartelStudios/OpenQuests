package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.openquests.extension.quests.quantity.HeldQuantityQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Counts the openings, or the picks of an answer, that its asset's filters let through.
 */
public class DialogueQuestProgression extends HeldQuantityQuestProgression<DialogueQuestProgression> {

    public static final BuilderCodec<DialogueQuestProgression> CODEC = BuilderCodec.builder(DialogueQuestProgression.class, DialogueQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                   .build();

    protected DialogueQuestProgression() {}

    @Override
    public OpenQuestsDialogueAsset getAsset() {
        return (OpenQuestsDialogueAsset) super.getAsset();
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null) return super.getDefaultTitle();

        String character = asset.getCharacter();
        String dialogue = asset.getDialogue();
        boolean answer = asset.getAnswer() != null;
        String key;
        if (character != null) key = answer ? "characterAnswer" : dialogue != null ? "characterDialogue" : "character";
        else if (dialogue != null) key = answer ? "answer" : "dialogue";
        else key = answer ? "anyAnswer" : "any";

        Message title = Message.translation("openquestsdialogues.quest.default." + key).param("quantity", getTargetQuantity());
        if (dialogue != null) title.param("dialogue", dialogueName(dialogue));
        if (character != null) title.param("character", characterName(character));
        return title;
    }

    @Nonnull
    private static Message dialogueName(@Nonnull String dialogueId) {
        OpenDialogueAsset dialogue = OpenDialogueAsset.getDialogue(dialogueId);
        return dialogue != null && dialogue.getTitle() != null ? Message.translation(dialogue.getTitle()) : Message.raw(dialogueId);
    }

    /**
     * The fixed name, else the epithet, never a drawn name, since one text serves every NPC carrying
     * the character; "someone" for a character with neither.
     */
    @Nonnull
    public static Message characterName(@Nonnull String characterId) {
        return Message.translation(characterNameKey(OpenDialogueCharacterAsset.getCharacter(characterId)));
    }

    /**
     * @return the translation key {@link #characterName} shows; {@code null} stands for an unknown character.
     */
    @Nonnull
    static String characterNameKey(@Nullable OpenDialogueCharacterAsset character) {
        if (character != null && character.getName() != null) return character.getName();
        if (character != null && character.getEpithet() != null) return character.getEpithet();
        return "openquestsdialogues.quest.default.someone";
    }
}
