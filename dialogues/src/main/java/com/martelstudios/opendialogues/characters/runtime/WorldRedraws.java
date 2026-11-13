package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.opendialogues.characters.CharacterAssets;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reaches a waiting NPC in its world: checks it, and lets it draw again as it would at spawn.
 */
public final class WorldRedraws implements VacancyPort<Waiter> {

    private static final Redraw NOTHING = new Redraw(false, List.of());

    private final Supplier<CharacterSettings> settings;
    private final CharacterService service;
    private final CharacterAssets assets;
    private final CharacterPositionsAccess positions;

    /**
     * @param settings read anew on each redraw, so stopping draws also stops redraws.
     */
    public WorldRedraws(@Nonnull Supplier<CharacterSettings> settings, @Nonnull CharacterService service, @Nonnull CharacterAssets assets, @Nonnull CharacterPositionsAccess positions) {
        this.settings = settings;
        this.service = service;
        this.assets = assets;
        this.positions = positions;
    }

    @Override
    public boolean execute(@Nonnull Waiter waiter, @Nonnull Runnable task) {
        if (!waiter.world().isAlive()) return false;
        waiter.world().execute(task);
        return true;
    }

    @Override
    public boolean isGone(@Nonnull Waiter waiter) {
        return !waiter.ref().isValid() || service.carriesCharacter(waiter.ref().getStore(), waiter.ref());
    }

    /**
     * Stopped draws make every character unavailable, so waiters keep their place until draws resume.
     */
    @Override
    public boolean isAvailable(@Nonnull Waiter waiter, @Nonnull String characterId) {
        if (!settings.get().isDraws()) return false;
        CharacterNpc npc = service.npc(waiter.ref().getStore(), waiter.ref());
        return npc != null && npc.unique().isAvailable(characterId, assets::character);
    }

    @Nonnull
    @Override
    public Redraw redraw(@Nonnull Waiter waiter) {
        Store<EntityStore> store = waiter.ref().getStore();
        NPCEntity entity = store.getComponent(waiter.ref(), NPCEntity.getComponentType());
        if (entity == null) return NOTHING;
        World world = waiter.world();
        Redraw redraw = service.edit(store, waiter.ref(), npc -> {
            CharacterAssigner.Draw draw = service.draw(npc, FilterContextFactory.of(store, waiter.ref(), world, entity));
            if (draw.character() != null) positions.record(store, waiter.ref(), npc);
            return new Redraw(draw.character() != null, ClaimSlot.all(draw.blocked(), assets::character, world.getWorldConfig().getUuid()));
        });
        return redraw == null ? NOTHING : redraw;
    }
}
