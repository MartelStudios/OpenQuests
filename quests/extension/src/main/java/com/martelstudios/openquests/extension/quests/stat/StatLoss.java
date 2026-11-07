package com.martelstudios.openquests.extension.quests.stat;

import com.hypixel.hytale.protocol.EntityStatOp;
import com.hypixel.hytale.protocol.EntityStatUpdate;
import it.unimi.dsi.fastutil.floats.FloatList;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What a stat lost during one tick, read from the change log an {@code EntityStatMap} keeps until
 * {@code EntityStatsSystems.ClearChanges} empties it. The log rather than the value: regeneration
 * landing in the same tick as a cost would otherwise hide part of it.
 */
public final class StatLoss {

    private StatLoss() {}

    /**
     * Adds up the falls of one stat's logged changes. A respawn reset and a maximum moving are not
     * the player losing anything, so their ops are left out.
     *
     * @param updates the stat's changes this tick, {@code null} when it did not change.
     * @param values  the value before and after each change, two floats per entry.
     * @return the points lost, never negative.
     */
    public static double measure(@Nullable List<EntityStatUpdate> updates, @Nullable FloatList values) {
        if (updates == null || values == null) return 0;

        double lost = 0;
        for (int i = 0; i < updates.size() && i * 2 + 1 < values.size(); i++) {
            if (!counts(updates.get(i).op)) continue;

            float before = values.getFloat(i * 2);
            float after = values.getFloat(i * 2 + 1);
            if (after < before) lost += before - after;
        }
        return lost;
    }

    private static boolean counts(@Nullable EntityStatOp op) {
        return op != EntityStatOp.Reset && op != EntityStatOp.Init && op != EntityStatOp.PutModifier && op != EntityStatOp.RemoveModifier;
    }
}
