package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Tells the players of a world that an NPC character in it died, so a quest aimed at it does not
 * stall without a reason.
 */
public final class CharacterDeathAnnouncer {

    private CharacterDeathAnnouncer() {}

    /**
     * Sends the death line to every player in the dead NPC's world, on that world's thread where the
     * release is dispatched.
     */
    public static void onReleased(@Nonnull CharacterEvent.Released event) {
        if (event.getReason() != ReleaseReason.Death || !event.getNpc().isValid()) return;

        Message message = message(event);
        if (message == null) return;
        for (PlayerRef player : event.getNpc().getStore().getExternalData().getWorld().getPlayerRefs()) player.sendMessage(message);
    }

    /**
     * Shared with the mods telling players elsewhere, so every death reads the same.
     *
     * @return the line for that death, {@code null} when the NPC has neither a name nor an epithet.
     */
    @Nullable
    public static Message message(@Nonnull CharacterEvent.Released event) {
        OpenDialogueCharacterAsset character = OpenDialogueCharacterAsset.getCharacter(event.getCharacter());
        return message(event.getName(), character == null ? null : character.getEpithet());
    }

    /**
     * @param name    the translation key of the drawn name, {@code null} for a nameless NPC.
     * @param epithet the translation key of the character's epithet, {@code null} for none.
     * @return the line for that death, {@code null} when there is neither.
     */
    @Nullable
    static Message message(@Nullable String name, @Nullable String epithet) {
        if (name == null && epithet == null) return null;

        String key = name == null ? "epithet" : epithet == null ? "name" : "named";
        Message message = Message.translation("opendialogues.character.death." + key);
        if (name != null) message.param("name", Message.translation(name));
        if (epithet != null) message.param("epithet", Message.translation(epithet));
        return message;
    }
}
