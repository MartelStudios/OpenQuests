package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.interactions.UseNPCInteraction;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.instructions.Instruction;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.OpenDialogueDistributionAsset;
import com.martelstudios.opendialogues.characters.OpenDialogueNameListAsset;

import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Every few seconds, lets the NPCs near each player of a world draw a character, and makes the
 * ones carrying a character answer F again after their chunk reloaded.
 */
public class CharacterScanSystem extends DelayedSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final double radius;
    private final ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;
    private final ResourceType<EntityStore, UniqueCharacters> uniqueType;

    public CharacterScanSystem(@Nonnull CharacterSettings settings, @Nonnull ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType, @Nonnull ResourceType<EntityStore, UniqueCharacters> uniqueType) {
        super(settings.getScanInterval());
        this.radius = settings.getScanRadius();
        this.identityType = identityType;
        this.uniqueType = uniqueType;
    }

    /**
     * Deferred past the tick, during which the store refuses the components an association adds.
     */
    @Override
    public void delayedTick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        if (world.getPlayerRefs().isEmpty()) return;
        world.execute(() -> scan(world, store));
    }

    private void scan(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        List<OpenDialogueDistributionAsset> distributions = List.copyOf(OpenDialogueDistributionAsset.getAssetStore().getAssetMap().getAssetMap().values());
        Set<Ref<EntityStore>> seen = new HashSet<>();
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            Ref<EntityStore> playerEntity = playerRef.getReference();
            if (playerEntity == null || !playerEntity.isValid()) continue;
            TransformComponent transform = store.getComponent(playerEntity, TransformComponent.getComponentType());
            Player player = store.getComponent(playerEntity, Player.getComponentType());
            if (transform == null || player == null) continue;

            List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
            store.getResource(NPCPlugin.get().getNpcSpatialResource()).getSpatialStructure().collect(transform.getPosition(), radius, nearby);
            for (Ref<EntityStore> npc : List.copyOf(nearby)) {
                if (npc.isValid() && seen.add(npc)) visit(world, store, npc, playerRef, player.getWorldMapTracker(), distributions);
            }
        }
    }

    private void visit(@Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull WorldMapTracker tracker, @Nonnull List<OpenDialogueDistributionAsset> distributions) {
        OpenDialoguesNPCIdentity stored = store.getComponent(ref, identityType);
        UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
        if (stored != null && stored.getCharacter() != null) {
            OpenDialogueCharacterAsset character = OpenDialogueCharacterAsset.getCharacter(stored.getCharacter());
            if (character != null && uuid != null) {
                CharacterContext context = new CharacterContext(store, ref, uuid.getUuid(), stored, character, CharacterAssets.live(), ThreadLocalRandom.current());
                for (CharacterTrait trait : character.getTraits()) trait.onApplied(context);
            }
            return;
        }

        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || uuid == null || !untouched(store, ref, npc)) return;

        OpenDialoguesNPCIdentity identity = stored == null ? new OpenDialoguesNPCIdentity() : (OpenDialoguesNPCIdentity) stored.clone();
        UniqueCharacters unique = store.getResource(uniqueType);
        CharacterAssigner.Draw draw = CharacterAssigner.assign(identity.getDraws(), distributions, context(store, ref, player, world, npc, tracker), id -> unique.isAvailable(id, OpenDialogueCharacterAsset::getCharacter), ThreadLocalRandom.current());
        if (draw.drawn().isEmpty()) return;
        draw.drawn().forEach(identity::recordDraw);
        String characterId = draw.character();
        identity.setCharacter(characterId);

        OpenDialogueCharacterAsset character = characterId == null ? null : OpenDialogueCharacterAsset.getCharacter(characterId);
        LOGGER.atFine().log("NPC %s (%s) drew %s.", uuid.getUuid(), npc.getRoleName(), characterId == null ? "nobody" : "character '" + characterId + "'");
        if (character != null) {
            if (character.getUniqueness().claims()) unique.claim(characterId, uuid.getUuid());
            CharacterContext context = new CharacterContext(store, ref, uuid.getUuid(), identity, character, CharacterAssets.live(), ThreadLocalRandom.current());
            for (CharacterTrait trait : character.getTraits()) trait.onAssigned(context);
            for (CharacterTrait trait : character.getTraits()) trait.onApplied(context);
        }
        store.putComponent(ref, identityType, identity);
    }

    /**
     * An NPC whose role answers F itself, or whose F another mod took, is not ours to change.
     */
    private static boolean untouched(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull NPCEntity npc) {
        Role role = npc.getRole();
        Instruction instruction = role == null ? null : role.getInteractionInstruction();
        if (instruction != null && instruction.componentCount() > 1) return false;
        Interactions interactions = store.getComponent(ref, Interactions.getComponentType());
        return interactions != null && UseNPCInteraction.DEFAULT_ID.equals(interactions.getInteractionId(InteractionType.Use));
    }

    @Nonnull
    private static FilterContext context(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world, @Nonnull NPCEntity npc, @Nonnull WorldMapTracker tracker) {
        WorldMapTracker.ZoneDiscoveryInfo zone = tracker.getCurrentZone();
        int roleIndex = npc.getRoleIndex();
        return new FilterContext(store, ref, player, world.getName(), npc.getRoleName(), zone == null ? null : zone.regionName(), tracker.getCurrentBiomeName(), group -> {
            int index = NPCGroup.getAssetMap().getIndexOrDefault(group, -1);
            return index >= 0 && WorldSupport.hasTagInGroup(index, roleIndex);
        });
    }

    /**
     * @return the NPCs' UUID, for callers holding only the entity.
     */
    @Nullable
    static UUID uuidOf(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
        return uuid == null ? null : uuid.getUuid();
    }
}
