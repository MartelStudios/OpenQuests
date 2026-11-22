package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestGiver;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.dialogues.givers.TargetMode;
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

    /** Translation key of the name shown for no one known. */
    public static final String SOMEONE = "openquestsdialogues.quest.default.someone";

    /** The tag an author writes the giver's fallback name in, read as the {@code {giver}} text parameter. */
    public static final String GIVER_TAG = "Text.giver";

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

        Message character = targetName(this, asset.getTargetMode(), asset.target(this));
        String dialogue = asset.getDialogue();
        boolean answer = asset.getAnswer() != null;
        String key;
        if (character != null) key = answer ? "characterAnswer" : dialogue != null ? "characterDialogue" : "character";
        else if (dialogue != null) key = answer ? "answer" : "dialogue";
        else key = answer ? "anyAnswer" : "any";

        Message title = Message.translation("openquestsdialogues.quest.default." + key).param("quantity", getTargetQuantity());
        if (dialogue != null) title.param("dialogue", dialogueName(dialogue));
        if (character != null) title.param("character", character);
        return title;
    }

    /**
     * A quest aimed at its giver shows the name its giver carries, else the {@code Text.giver} fallback
     * of its head, then of the step; otherwise the target character's name.
     *
     * @return the name of whom the quest aims at, {@code null} for no one.
     */
    @Nullable
    public static Message targetName(@Nonnull AbstractQuestProgression<?> quest, @Nonnull TargetMode mode, @Nullable QuestTarget target) {
        if (mode != TargetMode.Character) {
            QuestGiver giver = quest.getHead().getGiver();
            String name = giver != null ? giver.getNameKey() : null;
            if (name == null) name = giverName(quest.getHead().getTagValues(GIVER_TAG));
            if (name == null) name = giverName(quest.getTagValues(GIVER_TAG));
            if (name != null) return Message.translation(name);
        }
        String character = TargetMode.characterOf(target);
        return character == null ? null : characterName(character);
    }

    /**
     * @return the giver's name key in those {@code Text.giver} values, {@code null} for none.
     */
    @Nullable
    public static String giverName(@Nullable String[] values) {
        return values != null && values.length > 0 && !values[0].isEmpty() ? values[0] : null;
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
        return SOMEONE;
    }
}
