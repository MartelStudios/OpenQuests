package com.martelstudios.openquests.core.assignments.trigger;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * As a player connects, wherever they arrive.
 */
public class PlayerConnectTrigger extends AssignmentTrigger {

    public static final String TYPE = "PlayerConnect";

    public static final BuilderCodec<PlayerConnectTrigger> CODEC = BuilderCodec.builder(PlayerConnectTrigger.class, PlayerConnectTrigger::new, AssignmentTrigger.BASE_CODEC).build();

    @Nonnull
    @Override
    public Occasion onConnect(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
        return Occasion.connection(playerId, player);
    }
}
