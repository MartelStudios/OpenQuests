package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.openquests.extension.quests.quantity.HeldQuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.core.targets.TargetedQuest;
import com.martelstudios.openquests.dialogues.givers.TargetMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Open a dialogue, or pick one of its answers, a number of times. Character, dialogue and answer
 * each narrow what counts and are all optional, so none counts any conversation. With
 * {@code UseHistory}, what the player did before the quest counts too.
 */
public class OpenQuestsDialogueAsset extends HeldQuantityQuestAsset implements TargetedQuest {

    public static final BuilderCodec<OpenQuestsDialogueAsset> CODEC =
        BuilderCodec.builder(OpenQuestsDialogueAsset.class, OpenQuestsDialogueAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Character", Codec.STRING), (asset, character) -> asset.character = character, asset -> asset.character)
                    .documentation("ID of the character the conversation must be had with, read with Target Character; unset for any or none.")
                    .add()
                    .append(new KeyedCodec<>("Target", new EnumCodec<>(TargetMode.class)), (asset, target) -> asset.target = target, asset -> asset.target)
                    .documentation(TargetMode.DOCUMENTATION)
                    .documentation("NPC character ID to match, with no character restriction by default.")
                    .add()
                    .append(new KeyedCodec<>("Dialogue", Codec.STRING), (asset, dialogue) -> asset.dialogue = dialogue, asset -> asset.dialogue)
                    .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                    .documentation("Dialogue asset ID to match, with any conversation eligible by default.")
                    .add()
                    .append(new KeyedCodec<>("Answer", Codec.STRING), (asset, answer) -> asset.answer = answer, asset -> asset.answer)
                    .documentation("Response ID or one-based position within a specified dialogue, with openings counted by default.")
                    .add()
                    .append(new KeyedCodec<>("UseHistory", Codec.BOOLEAN), (asset, useHistory) -> asset.useHistory = useHistory, asset -> asset.useHistory)
                    .documentation("Counts matching interactions from before the quest when true, and defaults to false.")
                    .add()
                    .validator((asset, results) -> {
                        // A position only means something inside one dialogue.
                        if (asset.dialogue == null && asset.answer != null && asset.answer.chars().allMatch(Character::isDigit)) {
                            results.fail("Answer without Dialogue must be an answer Id, not a position");
                        }
                    })
                    .documentation("Defines a quest that counts dialogue openings or response selections.")
                    .build();

    /**
     * The NPC character the conversation must be had with, {@code null} for any or none.
     */
    @Nullable
    protected String character;

    @Nonnull
    protected TargetMode target = TargetMode.Character;

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
        return target == TargetMode.Character ? character : null;
    }

    /**
     * @return whom the quest aims at.
     */
    @Nonnull
    public TargetMode getTargetMode() {
        return target;
    }

    @Nullable
    @Override
    public QuestTarget target() {
        return getCharacter() == null ? null : new QuestTarget.Character(character);
    }

    @Nullable
    @Override
    public QuestTarget target(@Nonnull AbstractQuestProgression<?> quest) {
        return target.resolve(character, quest);
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
        return (character == null || character.equals(characterId)) && matchesMoment(dialogueId, answerKey);
    }

    private boolean matchesMoment(@Nonnull String dialogueId, @Nullable String answerKey) {
        return (dialogue == null || dialogue.equals(dialogueId)) && Objects.equals(answer, answerKey);
    }

    /**
     * @param target where the quest leads, from {@link #target(AbstractQuestProgression)}.
     * @param npcId  the NPC talked to, {@code null} without one.
     * @return whether that moment counts for that quest; one aimed at a giver it lacks counts nothing.
     */
    public boolean matches(@Nullable QuestTarget target, @Nullable String characterId, @Nullable UUID npcId, @Nonnull String dialogueId, @Nullable String answerKey) {
        if (this.target == TargetMode.Character) return matches(characterId, dialogueId, answerKey);
        return TargetMode.accepts(target, characterId, npcId) && matchesMoment(dialogueId, answerKey);
    }

    /**
     * @return how many times the player already did what this quest asks.
     */
    public int countIn(@Nonnull DialogueHistory history) {
        return history.count(character, dialogue, answer);
    }

    /**
     * @param target where the quest leads, from {@link #target(AbstractQuestProgression)}.
     * @return how many times the player already did what that quest asks, with whom it aims at.
     */
    public int countIn(@Nullable QuestTarget target, @Nonnull DialogueHistory history) {
        if (this.target == TargetMode.Character) return countIn(history);
        return switch (target) {
            case QuestTarget.Character one -> history.count(one.id(), dialogue, answer);
            case QuestTarget.Npc npc -> history.count(npc.character(), npc.npc(), dialogue, answer);
            case null, default -> 0;
        };
    }
}
