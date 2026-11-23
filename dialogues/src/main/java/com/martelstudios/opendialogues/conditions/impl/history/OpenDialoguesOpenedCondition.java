package com.martelstudios.opendialogues.conditions.impl.history;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.conditions.CountBounds;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.conditions.SourceScope;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.sessions.DialogueSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Holds while the player opened a dialogue between {@code Min} and {@code Max} times, inline
 * dialogues included.
 */
public class OpenDialoguesOpenedCondition extends DialogueCondition {

    public static final String TYPE = "Opened";

    public static final BuilderCodec<OpenDialoguesOpenedCondition> CODEC = BuilderCodec.builder(OpenDialoguesOpenedCondition.class, OpenDialoguesOpenedCondition::new, DialogueCondition.BASE_CODEC)
                                                                        .append(new KeyedCodec<>("Dialogue", Codec.STRING, true), (condition, dialogue) -> condition.dialogue = dialogue, condition -> condition.dialogue)
                                                                        .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                                                                        .add()
                                                                        .append(new KeyedCodec<>("Min", Codec.INTEGER), (condition, min) -> condition.min = min, condition -> condition.min)
                                                                        .add()
                                                                        .append(new KeyedCodec<>("Max", Codec.INTEGER), (condition, max) -> condition.max = max, condition -> condition.max)
                                                                        .add()
                                                                        .append(new KeyedCodec<>("With", new EnumCodec<>(SourceScope.class)), (condition, with) -> condition.with = with, condition -> condition.with)
                                                                        .documentation("Whose dialogues count: any NPC, the source's character, or the source NPC alone.")
                                                                        .add()
                                                                        .build();

    protected String dialogue;
    protected int min = 1;
    @Nullable
    protected Integer max;
    protected SourceScope with = SourceScope.Any;

    protected OpenDialoguesOpenedCondition() {}

    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        DialogueSource source = context.source();
        int opened = with.blind(source) ? 0 : context.history().count(with.character(source), with.npc(source), dialogue, null);
        return CountBounds.within(opened, min, max);
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        String bounds = CountBounds.validate(min, max);
        if (bounds != null) return bounds;
        return dialogues.apply(dialogue) == null ? "No dialogue '" + dialogue + "'" : null;
    }
}
