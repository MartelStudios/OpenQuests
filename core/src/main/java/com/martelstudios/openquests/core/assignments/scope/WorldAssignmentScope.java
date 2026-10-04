package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One quest per world, shared by everyone inside: the world the occasion happens in, or the open
 * worlds the pattern names, which is how entering one world can start a quest in others.
 */
public class WorldAssignmentScope extends AbstractWorldAssignmentScope {

    public static final String TYPE = "World";

    public static final BuilderCodec<WorldAssignmentScope> CODEC = BuilderCodec.builder(WorldAssignmentScope.class, WorldAssignmentScope::new, AbstractWorldAssignmentScope.BASE_CODEC).build();

    public WorldAssignmentScope() {}

    public WorldAssignmentScope(@Nullable String worldNamePattern) {
        super(worldNamePattern);
    }

    /**
     * Each world on its own thread, the only one allowed to touch it. The player whose entry this
     * is joins a quest created for the world they enter, being among its players only once in.
     */
    @Override
    public void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion) {
        QuestAssignmentService service = QuestAssignmentService.get();
        String key = occasion.timeKey();
        UUID entered = occasion.getWorld() == null ? null : occasion.getWorld().getWorldConfig().getUuid();

        for (World world : targets(occasion)) {
            UUID worldId = world.getWorldConfig().getUuid();
            if (occasion.isTimed() && service.isSettled(WorldQuestService.indexKey(world), assignment, key)) continue;

            UUID joining = worldId.equals(entered) ? occasion.getPlayerId() : null;
            WorldQuestService.onThreadOf(world, () -> service.offerAll(assignment, service.worldHolder(world, joining), key, occasion.isTimed()));
        }
    }

    /**
     * Without a pattern, the world the occasion happens in. With one, that world if it matches,
     * every open world it names otherwise: one opening later is reached by the next occasion.
     */
    @Nonnull
    private List<World> targets(@Nonnull Occasion occasion) {
        World place = occasion.getWorld();

        if (worldNamePattern == null) return place == null ? List.of() : List.of(place);
        if (place != null && worldNamePattern.matches(place.getName())) return List.of(place);

        List<World> worlds = new ArrayList<>();
        for (World world : Universe.get().getWorlds().values()) {
            if (worldNamePattern.matches(world.getName())) worlds.add(world);
        }
        return worlds;
    }
}
