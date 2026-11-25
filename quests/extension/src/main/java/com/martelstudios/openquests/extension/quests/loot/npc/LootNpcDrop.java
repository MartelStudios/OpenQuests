package com.martelstudios.openquests.extension.quests.loot.npc;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One source of a {@code LootNpc} item: which NPCs may drop it, how likely, how many, and after how
 * many kills without a drop it is forced.
 */
public class LootNpcDrop {

    public static final BuilderCodec<LootNpcDrop> CODEC = BuilderCodec.builder(LootNpcDrop.class, LootNpcDrop::new)
                                                                      .append(new KeyedCodec<>("NpcGroupId", NPCGroup.CHILD_ASSET_CODEC, true), (drop, groupId) -> drop.npcGroupId = groupId, drop -> drop.npcGroupId)
                                                                      .addValidator(Validators.nonNull())
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Chance", Codec.DOUBLE), (drop, chance) -> drop.chance = chance, drop -> Double.valueOf(drop.chance))
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Min", Codec.INTEGER), (drop, min) -> drop.min = min, drop -> Integer.valueOf(drop.min))
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Max", Codec.INTEGER), (drop, max) -> drop.max = max, drop -> Integer.valueOf(drop.max))
                                                                      .add()
                                                                      .append(new KeyedCodec<>("GuaranteedAfter", Codec.INTEGER), (drop, after) -> drop.guaranteedAfter = after, drop -> drop.guaranteedAfter)
                                                                      .add()
                                                                      .validator((drop, results) -> {
                                                                          String problem = drop.problem();
                                                                          if (problem != null) results.fail(problem);
                                                                      })
                                                                      .build();

    protected String npcGroupId;

    protected double chance = 1;

    protected int min = 1;

    protected int max = 1;

    @Nullable
    protected Integer guaranteedAfter;

    LootNpcDrop() {}

    /**
     * Checked once decoded, apart from the group, which only the asset store can resolve.
     *
     * @return what is wrong with the numbers, {@code null} when nothing is.
     */
    @Nullable
    String problem() {
        if (!(chance > 0 && chance <= 1)) return "Chance must be above 0 and at most 1";
        if (min < 1) return "Min must be at least 1";
        if (max < min) return "Max must not be below Min";
        if (guaranteedAfter != null && guaranteedAfter < 1) return "GuaranteedAfter must be at least 1";
        return null;
    }

    /**
     * @return the NPC group whose members may drop the item.
     */
    @Nonnull
    public String getNpcGroupId() {
        return npcGroupId;
    }

    /**
     * @return the chance a kill drops the item, above 0 and at most 1.
     */
    public double getChance() {
        return chance;
    }

    /**
     * @return the fewest items a drop gives.
     */
    public int getMin() {
        return min;
    }

    /**
     * @return the most items a drop gives.
     */
    public int getMax() {
        return max;
    }

    /**
     * @return the kill, counted in a row without a drop, that is forced to drop, {@code null} for none.
     */
    @Nullable
    public Integer getGuaranteedAfter() {
        return guaranteedAfter;
    }
}
