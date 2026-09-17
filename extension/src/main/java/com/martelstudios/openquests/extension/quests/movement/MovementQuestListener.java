package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.extension.listener.QuestListenerComponent;

import javax.annotation.Nonnull;

/**
 * The movement quests a player is running, whatever their pace and whether they count steps or
 * jumps — the kind is registered on {@link MovementQuestProgression}, so a quest type built on it
 * elsewhere is followed here too without saying anything.
 */
public class MovementQuestListener extends QuestListenerComponent {

    /**
     * As far as a player could plausibly move between two ticks. Anything beyond is a teleport or
     * a change of world, which would otherwise pay like a journey.
     */
    private static final double MAX_STEP = 8;

    private double lastX;
    private double lastZ;
    private boolean sampled;

    public static ComponentType<EntityStore, MovementQuestListener> getComponentType() {
        return MovementFeature.getListenerType();
    }

    /**
     * Measures the ground covered since the last sample and remembers where the player is now.
     * Kept here rather than worked out from their velocity: what the server holds under that name
     * is the impulses acting on them, not the walking their own client drives.
     *
     * @return the flat distance travelled, zero for the first sample and for a jump in position no
     * walk accounts for. Counting the climb too would have a ladder pay the same as a road.
     */
    public double sampleTravel(double x, double z) {
        double travelled = 0;

        if (sampled) {
            double dx = x - lastX;
            double dz = z - lastZ;
            double squared = dx * dx + dz * dz;

            if (squared <= MAX_STEP * MAX_STEP) travelled = Math.sqrt(squared);
        }

        lastX = x;
        lastZ = z;
        sampled = true;

        return travelled;
    }

    @Nonnull
    @Override
    protected QuestListenerComponent create() {
        return new MovementQuestListener();
    }
}
