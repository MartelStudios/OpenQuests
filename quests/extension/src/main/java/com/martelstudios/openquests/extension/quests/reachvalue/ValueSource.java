package com.martelstudios.openquests.extension.quests.reachvalue;

import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * A value of the game a {@code ReachValue} quest watches, read again every tick since none of them
 * fires an event when it changes.
 */
public abstract class ValueSource {

    /**
     * Polymorphic dispatcher: each kind registers under a {@code "Type"} tag, so a plugin can add
     * its own.
     */
    public static final CodecMapCodec<ValueSource> CODEC = new CodecMapCodec<>("Type");

    /**
     * Reads the value for one player, on the world thread.
     *
     * @return the current value, 0 when there is nothing to read yet.
     */
    public abstract int read(@Nonnull Ref<EntityStore> player, @Nonnull Store<EntityStore> store);

    /**
     * @return a title naming the value and the target to reach, for a quest without its own.
     */
    @Nonnull
    public abstract Message title(int target);
}
