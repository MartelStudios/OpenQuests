package com.martelstudios.opendialogues.conditions.impl.history;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.conditions.SourceScope;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.sessions.DialogueSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.function.Function;

/**
 * Holds when the player's latest opening of a dialogue is more recent than their latest opening of
 * another, or the other was never opened: an offer made since the last thanks, a visit since a warning.
 */
public class OpenDialoguesOpenedAfterCondition extends DialogueCondition {

    public static final String TYPE = "OpenedAfter";

    public static final BuilderCodec<OpenDialoguesOpenedAfterCondition> CODEC = BuilderCodec.builder(OpenDialoguesOpenedAfterCondition.class, OpenDialoguesOpenedAfterCondition::new, DialogueCondition.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("Dialogue", Codec.STRING, true), (condition, dialogue) -> condition.dialogue = dialogue, condition -> condition.dialogue)
                                                                              .addValidator(Validators.nonNull())
                                                                              .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                                                                              .documentation("ID of the dialogue whose latest opening must be the more recent.")
                                                                              .add()
                                                                              .append(new KeyedCodec<>("Than", Codec.STRING, true), (condition, than) -> condition.than = than, condition -> condition.than)
                                                                              .addValidator(Validators.nonNull())
                                                                              .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                                                                              .documentation("ID of the dialogue opened earlier, or never.")
                                                                              .add()
                                                                              .append(new KeyedCodec<>("With", new EnumCodec<>(SourceScope.class)), (condition, with) -> condition.with = with, condition -> condition.with)
                                                                              .documentation("Whose dialogues count: any NPC, the source's character, or the source NPC alone.")
                                                                              .add()
                                                                              .documentation("Holds when Dialogue was last opened after Than, or Than was never opened.")
                                                                              .build();

    private String dialogue;
    private String than;
    private SourceScope with = SourceScope.Any;

    protected OpenDialoguesOpenedAfterCondition() {}

    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        DialogueSource source = context.source();
        if (with.blind(source)) return false;

        Instant last = lastOpened(context.history(), source, dialogue);
        Instant earlier = lastOpened(context.history(), source, than);
        return last != null && (earlier == null || last.isAfter(earlier));
    }

    @Nullable
    private Instant lastOpened(@Nonnull DialogueHistory history, @Nullable DialogueSource source, @Nonnull String dialogueId) {
        return with == SourceScope.Any
               ? history.lastOccurrence(null, dialogueId, null)
               : history.lastOccurrence(with.character(source), with.npc(source), dialogueId, null);
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        if (dialogue == null || dialogue.isBlank() || than == null || than.isBlank()) return "Dialogue and Than must be specified";
        if (dialogues.apply(dialogue) == null) return "No dialogue '" + dialogue + "'";
        return dialogues.apply(than) == null ? "No dialogue '" + than + "'" : null;
    }
}
