package com.martelstudios.opendialogues.dialogues;

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
     * Only a speaker allowing the NPC override takes the NPC's name and its character speaker's
     * fields, each one it does not set falling back to the speaker's own.
     *
     * @param characterSpeakers the speaker a character lends, by character id, {@code null} for none.
     * @return what to show, {@code null} without a speaker.
     */
    @Nullable
    public static SpeakerView of(@Nullable OpenDialogueSpeakerAsset speaker, @Nullable DialogueSource source, @Nonnull Function<String, OpenDialogueSpeakerAsset> characterSpeakers) {
        if (speaker == null) return null;
        if (source == null || !speaker.isAllowNPCOverride()) return new SpeakerView(speaker.getName(), speaker.getEpithet(), speaker.getPortrait());

        OpenDialogueSpeakerAsset lent = characterSpeakers.apply(source.character());
        return new SpeakerView(
            firstSet(source.name(), lent == null ? null : lent.getName(), speaker.getName()),
            firstSet(lent == null ? null : lent.getEpithet(), speaker.getEpithet()),
            firstSet(lent == null ? null : lent.getPortrait(), speaker.getPortrait()));
    }

    @Nullable
    private static String firstSet(@Nullable String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) return value;
        }
        return null;
    }
}
