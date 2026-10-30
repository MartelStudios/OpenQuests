package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

/**
 * Counts the openings of its dialogue, or the picks of its answer.
 */
public class DialogueQuestProgression extends QuantityQuestProgression<DialogueQuestProgression> {

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
        if (asset == null || asset.getDialogue() == null) return super.getDefaultTitle();

        OpenDialogueAsset dialogue = OpenDialogueAsset.getDialogue(asset.getDialogue());
        Message name = dialogue != null && dialogue.getTitle() != null ? Message.translation(dialogue.getTitle()) : Message.raw(asset.getDialogue());
        String key = asset.getAnswer() == null ? "openquestsdialogues.quest.default.dialogue" : "openquestsdialogues.quest.default.answer";
        return Message.translation(key).param("quantity", getTargetQuantity()).param("dialogue", name);
    }
}
