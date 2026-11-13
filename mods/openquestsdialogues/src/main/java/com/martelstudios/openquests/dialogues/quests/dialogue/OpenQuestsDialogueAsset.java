package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.openquests.extension.quests.quantity.HeldQuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.core.targets.TargetedQuest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Open a dialogue, or pick one of its answers, a number of times. Character, dialogue and answer
 * each narrow what counts and are all optional, so none counts any conversation. With
 * {@code UseHistory}, what the player did before the quest counts too.
 */
public class OpenQuestsDialogueAsset extends HeldQuantityQuestAsset implements TargetedQuest {

    public static final BuilderCodec<OpenQuestsDialogueAsset> CODEC =
        BuilderCodec.builder(OpenQuestsDialogueAsset.class, OpenQuestsDialogueAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Character", Codec.STRING), (asset, character) -> asset.character = character, asset -> asset.character)
                    .add()
                    .append(new KeyedCodec<>("Dialogue", Codec.STRING), (asset, dialogue) -> asset.dialogue = dialogue, asset -> asset.dialogue)
                    .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                    .add()
                    .append(new KeyedCodec<>("Answer", Codec.STRING), (asset, answer) -> asset.answer = answer, asset -> asset.answer)
                    .add()
                    .append(new KeyedCodec<>("UseHistory", Codec.BOOLEAN), (asset, useHistory) -> asset.useHistory = useHistory, asset -> asset.useHistory)
                    .add()
                    .validator((asset, results) -> {
                        // A position only means something inside one dialogue.
                        if (asset.dialogue == null && asset.answer != null && asset.answer.chars().allMatch(Character::isDigit)) {
                            results.fail("Answer without Dialogue must be an answer Id, not a position");
                        }
                    })
                    .build();

    /**
     * The NPC character the conversation must be had with, {@code null} for any or none.
     */
    @Nullable
    protected String character;

    /**
     * A named dialogue: an inline one gets a generated id that moves with its file. {@code null} for any.
     */
    @Nullable
    protected String dialogue;

    /**
     * The answer's {@code Id}, or its position in the file from 1; {@code null} counts openings.
     */
    @Nullable
    protected String answer;

    protected boolean useHistory;

    OpenQuestsDialogueAsset() {}

    @Override
    public QuantityQuestProgression.Builder<DialogueQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new DialogueQuestProgression(), getId());
    }

    /**
     * Read by the death notices to find who holds a quest aimed at a character that just died.
     */
    @Nullable
    public String getCharacter() {
        return character;
    }

    @Nullable
    @Override
    public QuestTarget target() {
        return character == null ? null : new QuestTarget.Character(character);
    }

    @Nullable
    public String getDialogue() {
        return dialogue;
    }

    @Nullable
    public String getAnswer() {
        return answer;
    }

    public boolean isUseHistory() {
        return useHistory;
    }

    /**
     * @param characterId the character of the NPC talked to, {@code null} without one.
     * @param answerKey   the key of the answer picked, {@code null} for an opening.
     * @return whether that moment counts here.
     */
    public boolean matches(@Nullable String characterId, @Nonnull String dialogueId, @Nullable String answerKey) {
        return (character == null || character.equals(characterId))
               && (dialogue == null || dialogue.equals(dialogueId))
               && Objects.equals(answer, answerKey);
    }

    /**
     * @return how many times the player already did what this quest asks.
     */
    public int countIn(@Nonnull DialogueHistory history) {
        return history.count(character, dialogue, answer);
    }
}
