package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.opendialogues.characters.CharacterAssets;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * Gives an NPC its character as it enters the world, spawned or loaded with its chunk, so players
 * never see the draw: one without a character may draw one, one carrying a character gets its traits
 * applied again.
 */
public class CharacterSpawnSystem extends RefSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Supplier<CharacterSettings> settings;
    private final CharacterService service;
    private final CharacterPositionsAccess positions;
    private final CharacterWaitlist<Waiter> waitlist;
    private final CharacterAssets assets;

    /**
     * @param settings  read anew on each NPC, so stopping draws needs no restart.
     * @param positions where an NPC carrying a character is recorded, so its marker shows at once.
     * @param waitlist  where an NPC refused a taken unique character waits for it to be freed.
     */
    public CharacterSpawnSystem(@Nonnull Supplier<CharacterSettings> settings, @Nonnull CharacterService service, @Nonnull CharacterPositionsAccess positions, @Nonnull CharacterWaitlist<Waiter> waitlist, @Nonnull CharacterAssets assets) {
        this.settings = settings;
        this.service = service;
        this.positions = positions;
        this.waitlist = waitlist;
        this.assets = assets;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    /**
     * Deferred past the tick, during which the store refuses the components traits add.
     */
    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (ref.isValid()) visit(world, store, ref);
        });
    }

    /**
     * Any removal, an unload included: a reloaded NPC draws again and enlists anew.
     */
    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        waitlist.forget(new Waiter(ref, store.getExternalData().getWorld()));
    }

    private void visit(@Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        NPCEntity entity = store.getComponent(ref, NPCEntity.getComponentType());
        if (entity == null) return;
        service.edit(store, ref, npc -> {
            if (npc.identity().getCharacter() != null) {
                service.apply(npc);
            } else if (settings.get().isDraws()) {
                int drawsBefore = npc.identity().getDraws().size();
                CharacterAssigner.Draw draw = service.draw(npc, FilterContextFactory.of(store, ref, world, entity));
                String drawn = draw.character();
                waitlist.waitFor(new Waiter(ref, world), ClaimSlot.all(draw.blocked(), assets::character, world.getWorldConfig().getUuid()));
                if (npc.identity().getDraws().size() != drawsBefore) LOGGER.atFine().log("NPC %s (%s) drew %s.", npc.uuid(), entity.getRoleName(), drawn == null ? "nobody" : "character '" + drawn + "'");
            }
            positions.record(store, ref, npc);
            return null;
        });
    }
}
