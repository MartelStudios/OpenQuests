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
 * Holds while the player picked an answer between {@code Min} and {@code Max} times. The answer is
 * named by its {@code Id}, or by its position in the file from 1 when it has none.
 */
public class OpenDialoguesAnsweredCondition extends DialogueCondition {

    public static final String TYPE = "Answered";

    public static final BuilderCodec<OpenDialoguesAnsweredCondition> CODEC = BuilderCodec.builder(OpenDialoguesAnsweredCondition.class, OpenDialoguesAnsweredCondition::new, DialogueCondition.BASE_CODEC)
                                                                            .append(new KeyedCodec<>("Dialogue", Codec.STRING, true), (condition, dialogue) -> condition.dialogue = dialogue, condition -> condition.dialogue)
                                                                            .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                                                                            .documentation("ID of the conversation to check in the player's history.")
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Answer", Codec.STRING, true), (condition, answer) -> condition.answer = answer, condition -> condition.answer)
                                                                            .documentation("Identifier of the choice, using its one-based position if it has no explicit identifier.")
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Min", Codec.INTEGER), (condition, min) -> condition.min = min, condition -> condition.min)
                                                                            .documentation("Minimum number of selections required, defaulting to 1.")
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Max", Codec.INTEGER), (condition, max) -> condition.max = max, condition -> condition.max)
                                                                            .documentation("Maximum number of selections allowed, with no upper limit by default.")
                                                                            .add()
                                                                            .append(new KeyedCodec<>("With", new EnumCodec<>(SourceScope.class)), (condition, with) -> condition.with = with, condition -> condition.with)
                                                                            .documentation("Whose dialogues count: any NPC, the source's character, or the source NPC alone.")
                                                                            .add()
                                                                            .documentation("Tests how often the player chose a particular answer.")
                                                                            .build();

    protected String dialogue;
    protected String answer;
    protected int min = 1;
    @Nullable
    protected Integer max;
    protected SourceScope with = SourceScope.Any;

    protected OpenDialoguesAnsweredCondition() {}

    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        DialogueSource source = context.source();
        int answered = with.blind(source) ? 0 : context.history().count(with.character(source), with.npc(source), dialogue, answer);
        return CountBounds.within(answered, min, max);
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        String bounds = CountBounds.validate(min, max);
        if (bounds != null) return bounds;
        OpenDialogueAsset asset = dialogues.apply(dialogue);
        if (asset == null) return "No dialogue '" + dialogue + "'";
        return asset.findAnswer(answer) == null ? "Dialogue '" + dialogue + "' has no answer '" + answer + "'" : null;
    }
}
