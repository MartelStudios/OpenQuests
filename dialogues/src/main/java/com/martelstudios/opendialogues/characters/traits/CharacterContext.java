package com.martelstudios.opendialogues.characters.traits;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.runtime.OpenDialoguesNPCIdentity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * The NPC a trait hook acts on, valid for that call only.
 *
 * @param accessor  the store to read and write the NPC's components with: a command buffer when the
 *                  store is ticking (use, death, removal), the store otherwise; {@code null} in unit tests.
 * @param identity  who the NPC became; a hook may change it, the service saves it.
 * @param character the character the NPC carries.
 * @param random    the randomness a trait draws with, so draws stay testable.
 */
public record CharacterContext(@Nullable ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> npc, @Nonnull UUID uuid, @Nonnull OpenDialoguesNPCIdentity identity, @Nonnull OpenDialogueCharacterAsset character, @Nonnull CharacterAssets assets, @Nonnull RandomGenerator random) {}
