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
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.OpenDialogueDistributionAsset;
import com.martelstudios.opendialogues.characters.OpenDialogueNameListAsset;

import com.martelstudios.opendialogues.characters.filters.FilterContext;

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

    /**
     * Shown when a character names no hint of its own.
     */
    public static final String DEFAULT_HINT = "server.interactionHints.generic";

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
                if (npc.isValid() && seen.add(npc)) visit(world, store, npc, player.getWorldMapTracker(), distributions);
            }
        }
    }

    private void visit(@Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull WorldMapTracker tracker, @Nonnull List<OpenDialogueDistributionAsset> distributions) {
        OpenDialoguesNPCIdentity stored = store.getComponent(ref, identityType);
        if (stored != null && stored.getCharacter() != null) {
            OpenDialogueCharacterAsset character = OpenDialogueCharacterAsset.getCharacter(stored.getCharacter());
            if (character != null) makeTalkative(store, ref, character, stored.getName());
            return;
        }

        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
        if (npc == null || uuid == null || !untouched(store, ref, npc)) return;

        OpenDialoguesNPCIdentity identity = stored == null ? new OpenDialoguesNPCIdentity() : (OpenDialoguesNPCIdentity) stored.clone();
        UniqueCharacters unique = store.getResource(uniqueType);
        CharacterAssigner.Draw draw = CharacterAssigner.assign(identity.getDraws(), distributions, context(world, npc, tracker), id -> unique.isAvailable(id, OpenDialogueCharacterAsset::getCharacter), ThreadLocalRandom.current());
        if (draw.drawn().isEmpty()) return;
        draw.drawn().forEach(identity::recordDraw);
        String characterId = draw.character();
        identity.setCharacter(characterId);

        OpenDialogueCharacterAsset character = characterId == null ? null : OpenDialogueCharacterAsset.getCharacter(characterId);
        LOGGER.atFine().log("NPC %s (%s) drew %s.", uuid.getUuid(), npc.getRoleName(), characterId == null ? "nobody" : "character '" + characterId + "'");
        if (character != null) {
            identity.setName(drawName(character));
            if (character.getUniqueness().claims()) unique.claim(characterId, uuid.getUuid());
            if (identity.getName() != null) {
                Message name = Message.translation(identity.getName());
                store.putComponent(ref, PersistentDisplayName.getComponentType(), new PersistentDisplayName(name));
                store.putComponent(ref, DisplayNameComponent.getComponentType(), new DisplayNameComponent(name));
            }
            makeTalkative(store, ref, character, identity.getName());
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
    private static FilterContext context(@Nonnull World world, @Nonnull NPCEntity npc, @Nonnull WorldMapTracker tracker) {
        WorldMapTracker.ZoneDiscoveryInfo zone = tracker.getCurrentZone();
        int roleIndex = npc.getRoleIndex();
        return new FilterContext(world.getName(), npc.getRoleName(), zone == null ? null : zone.regionName(), tracker.getCurrentBiomeName(), group -> {
            int index = NPCGroup.getAssetMap().getIndexOrDefault(group, -1);
            return index >= 0 && WorldSupport.hasTagInGroup(index, roleIndex);
        });
    }

    @Nullable
    private static String drawName(@Nonnull OpenDialogueCharacterAsset character) {
        OpenDialogueNameListAsset list = character.getNames() == null ? null : OpenDialogueNameListAsset.getNameList(character.getNames());
        if (list == null || list.getNames().length == 0) return null;
        return list.getNames()[ThreadLocalRandom.current().nextInt(list.getNames().length)];
    }

    /**
     * Interactable and the nameplate are ensured on every scan, since nothing says they outlive a
     * chunk reload. A nameplate is plain text, so the name shows in the server language.
     */
    private static void makeTalkative(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull OpenDialogueCharacterAsset character, @Nullable String name) {
        if (name != null && store.getComponent(ref, Nameplate.getComponentType()) == null) {
            String text = I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, name);
            store.putComponent(ref, Nameplate.getComponentType(), new Nameplate(text != null ? text : name));
        }
        store.ensureComponent(ref, Interactable.getComponentType());
        Interactions interactions = store.getComponent(ref, Interactions.getComponentType());
        String hint = hintOf(character);
        if (interactions != null && !hint.equals(interactions.getInteractionHint())) interactions.setInteractionHint(hint);
    }

    /**
     * @return the translation key of the F prompt the character shows.
     */
    @Nonnull
    static String hintOf(@Nonnull OpenDialogueCharacterAsset character) {
        return character.getHint() != null ? character.getHint() : DEFAULT_HINT;
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
