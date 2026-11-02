package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Settles the universe characters held in a world closing for good. A crash or a server stop
 * raises no such removal, so the claims stay for the world coming back.
 */
public class CharacterWorldClosing {

    private final Supplier<UniverseCharacters> universe;
    private final Function<String, OpenDialogueCharacterAsset> characters;
    private final IEventBus events;

    public CharacterWorldClosing(@Nonnull Supplier<UniverseCharacters> universe, @Nonnull Function<String, OpenDialogueCharacterAsset> characters, @Nonnull IEventBus events) {
        this.universe = universe;
        this.characters = characters;
        this.events = events;
    }

    /**
     * Listened last, so a removal another plugin called off is seen as such.
     */
    public void onRemoveWorld(@Nonnull RemoveWorldEvent event) {
        if (event.isCancelled() || event.getRemovalReason() != RemoveWorldEvent.RemovalReason.GENERAL) return;
        close(event.getWorld().getWorldConfig().getUuid());
    }

    void close(@Nonnull UUID world) {
        for (String characterId : universe.get().closeWorld(world, characters)) {
            events.dispatchFor(CharacterEvent.Lost.class, characterId).dispatch(new CharacterEvent.Lost(characterId, CharacterEvent.Lost.Cause.WorldClosed));
        }
    }
}
