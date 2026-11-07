package com.martelstudios.opendialogues.dialogues;

import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.sessions.DialogueSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Who a display shows speaking, once the NPC a conversation came from had its say.
 *
 * @param name     the translation key of the name, {@code null} for none.
 * @param epithet  the translation key of the epithet, {@code null} for none.
 * @param portrait the portrait image relative to {@code Common/}, {@code null} for none.
 */
public record SpeakerView(@Nullable String name, @Nullable String epithet, @Nullable String portrait) {

    /**
     * A dialogue using the source character shows the source's resolved name and epithet and its
     * character's portrait, each one unset falling back to the dialogue's own character.
     *
     * @param characters a character by id, {@code null} for one no pack declares.
     * @return what to show, {@code null} when nobody speaks.
     */
    @Nullable
    public static SpeakerView of(@Nullable String characterId, boolean useSource, @Nullable DialogueSource source, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        OpenDialogueCharacterAsset character = characterId == null ? null : characters.apply(characterId);
        if (useSource && source != null) {
            OpenDialogueCharacterAsset sourceCharacter = characters.apply(source.character());
            return new SpeakerView(
                firstSet(source.name(), character == null ? null : character.getName()),
                firstSet(source.epithet(), character == null ? null : character.getEpithet()),
                firstSet(sourceCharacter == null ? null : sourceCharacter.getPortrait(), character == null ? null : character.getPortrait()));
        }
        if (character == null) return null;
        return new SpeakerView(character.getName(), character.getEpithet(), character.getPortrait());
    }

    @Nullable
    private static String firstSet(@Nullable String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) return value;
        }
        return null;
    }
}
