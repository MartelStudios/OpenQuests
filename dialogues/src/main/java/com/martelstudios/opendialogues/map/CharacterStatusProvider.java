package com.martelstudios.opendialogues.map;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

/**
 * What another mod says of characters for one player, such as a quest leading to them. Asked in one
 * call per player, so its cost does not grow with NPCs. Called on the player's world thread.
 */
@FunctionalInterface
public interface CharacterStatusProvider {

    /**
     * @return a status per character id; a character with a status shows even before it is met.
     */
    @Nonnull
    Map<String, CharacterMapStatus> statusesFor(@Nonnull UUID playerId);

    /**
     * @return a status per NPC UUID, shown on that NPC's marker alone, over its character's.
     */
    @Nonnull
    default Map<UUID, CharacterMapStatus> npcStatusesFor(@Nonnull UUID playerId) {
        return Map.of();
    }
}
