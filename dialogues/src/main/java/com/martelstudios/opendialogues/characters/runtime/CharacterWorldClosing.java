package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Settles the characters held in a world closing for good: the universe claims, and every NPC
 * carrying one, released as its world will never load again. A crash or a server stop raises no
 * such removal, so all stays for the world coming back.
 */
public class CharacterWorldClosing {

    private final Supplier<UniverseCharacters> universe;
    private final Function<String, OpenDialogueCharacterAsset> characters;
    private final Supplier<ReleasedNpcs> released;
    private final Function<World, Set<UUID>> npcsOf;
    private final IEventBus events;

    /**
     * @param npcsOf the NPCs carrying a character in a world, loaded or not.
     */
    public CharacterWorldClosing(@Nonnull Supplier<UniverseCharacters> universe, @Nonnull Function<String, OpenDialogueCharacterAsset> characters, @Nonnull Supplier<ReleasedNpcs> released, @Nonnull Function<World, Set<UUID>> npcsOf, @Nonnull IEventBus events) {
        this.universe = universe;
        this.characters = characters;
        this.released = released;
        this.npcsOf = npcsOf;
        this.events = events;
    }

    /**
     * Listened last, so a removal another plugin called off is seen as such.
     */
    public void onRemoveWorld(@Nonnull RemoveWorldEvent event) {
        if (event.isCancelled() || event.getRemovalReason() != RemoveWorldEvent.RemovalReason.GENERAL) return;
        close(event.getWorld().getWorldConfig().getUuid(), npcsOf.apply(event.getWorld()));
    }

    void close(@Nonnull UUID world, @Nonnull Set<UUID> npcs) {
        for (String characterId : universe.get().closeWorld(world, characters)) {
            events.dispatchFor(CharacterEvent.Lost.class, characterId).dispatch(new CharacterEvent.Lost(characterId, CharacterEvent.Lost.Cause.WorldClosed));
        }
        if (npcs.isEmpty()) return;

        released.get().addAll(npcs, Instant.now());
        events.dispatchFor(CharacterEvent.WorldReleased.class, null).dispatch(new CharacterEvent.WorldReleased(npcs));
    }
}
