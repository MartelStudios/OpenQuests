package com.martelstudios.opendialogues.conditions.impl.history;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.history.DialogueHistory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Holds unless the delay after the player's latest matching dialogue interaction is running:
 * before any such interaction and once the delay ran out.
 */
public class OpenDialoguesCooldownCondition extends DialogueCondition {

    public static final String TYPE = "Cooldown";

    public static final BuilderCodec<OpenDialoguesCooldownCondition> CODEC = BuilderCodec.builder(OpenDialoguesCooldownCondition.class, OpenDialoguesCooldownCondition::new, DialogueCondition.BASE_CODEC)
                                                                            .append(new KeyedCodec<>("Dialogue", Codec.STRING, true), (condition, dialogue) -> condition.dialogue = dialogue, condition -> condition.dialogue)
                                                                            .addValidator(Validators.nonNull())
                                                                            .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Answer", Codec.STRING), (condition, answer) -> condition.answer = answer, condition -> condition.answer)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Character", Codec.STRING), (condition, character) -> condition.character = character, condition -> condition.character)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Seconds", Codec.DURATION_SECONDS, true), (condition, duration) -> condition.duration = duration, condition -> condition.duration)
                                                                            .addValidator(Validators.nonNull())
                                                                            .addValidator(Validators.greaterThan(Duration.ZERO))
                                                                            .add()
                                                                            .build();

    private String dialogue;
    private String answer;
    private String character;
    private Duration duration;

    protected OpenDialoguesCooldownCondition() {}

    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        return !isActive(context.history(), Instant.now());
    }

    boolean isActive(@Nonnull DialogueHistory history, @Nonnull Instant now) {
        Instant last = history.lastOccurrence(character, dialogue, answer);
        return last != null && Duration.between(last, now).compareTo(duration) < 0;
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return validate(dialogues, OpenDialogueCharacterAsset::getCharacter);
    }

    @Nullable
    String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues,
                    @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        if (duration == null || duration.compareTo(Duration.ZERO) <= 0) return "Seconds must be positive";
        if (dialogue == null || dialogue.isBlank()) return "Dialogue must be specified";
        if (character != null && (character.isBlank() || characters.apply(character) == null)) {
            return "No character '" + character + "'";
        }
        OpenDialogueAsset asset = dialogues.apply(dialogue);
        if (asset == null) return "No dialogue '" + dialogue + "'";
        return answer != null && asset.findAnswer(answer) == null
               ? "Dialogue '" + dialogue + "' has no answer '" + answer + "'"
               : null;
    }
}
