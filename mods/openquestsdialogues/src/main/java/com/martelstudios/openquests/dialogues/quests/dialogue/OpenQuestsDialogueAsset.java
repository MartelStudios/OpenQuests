package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Open a dialogue, or pick one of its answers, a number of times. With {@code UseHistory}, what the
 * player did before the quest counts too.
 */
public class OpenQuestsDialogueAsset extends QuantityQuestAsset {

    public static final BuilderCodec<OpenQuestsDialogueAsset> CODEC =
        BuilderCodec.builder(OpenQuestsDialogueAsset.class, OpenQuestsDialogueAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Dialogue", Codec.STRING, true), (asset, dialogue) -> asset.dialogue = dialogue, asset -> asset.dialogue)
                    .addValidator(Validators.nonNull())
                    .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                    .add()
                    .append(new KeyedCodec<>("Answer", Codec.STRING), (asset, answer) -> asset.answer = answer, asset -> asset.answer)
                    .add()
                    .append(new KeyedCodec<>("UseHistory", Codec.BOOLEAN), (asset, useHistory) -> asset.useHistory = useHistory, asset -> asset.useHistory)
                    .add()
                    .build();

    /**
     * A named dialogue: an inline one gets a generated id that moves with its file.
     */
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
     * @param answerKey the key of the answer picked, {@code null} for an opening.
     * @return whether that moment counts here.
     */
    public boolean matches(@Nonnull String dialogueId, @Nullable String answerKey) {
        return dialogue.equals(dialogueId) && Objects.equals(answer, answerKey);
    }

    /**
     * @return how many times the player already did what this quest asks.
     */
    public int countIn(@Nonnull DialogueHistory history) {
        return answer == null ? history.opened(dialogue) : history.answered(dialogue, answer);
    }
}
