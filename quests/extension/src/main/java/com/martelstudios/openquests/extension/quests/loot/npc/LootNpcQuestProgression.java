package com.martelstudios.openquests.extension.quests.loot.npc;

import com.hypixel.hytale.builtin.tagset.TagSetPlugin;
import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Counts the items received from this quest's drops, and per source the kills in a row without
 * one, so a guarantee survives a restart.
 */
public class LootNpcQuestProgression extends QuantityQuestProgression<LootNpcQuestProgression> {

    public static final BuilderCodec<LootNpcQuestProgression> CODEC = BuilderCodec.builder(LootNpcQuestProgression.class, LootNpcQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("Misses", Codec.INT_ARRAY), (quest, misses) -> quest.misses = misses, quest -> quest.misses.length == 0 ? null : quest.misses)
                                                                                  .add()
                                                                                  .build();

    /**
     * Indexed like the asset's {@code Drops}; grown on demand, so an entry added to the asset later reads 0.
     */
    private int[] misses = new int[0];

    protected LootNpcQuestProgression() {}

    @Override
    public LootNpcQuestAsset getAsset() {
        return (LootNpcQuestAsset) super.getAsset();
    }

    /**
     * @return the kills in a row without a drop counted against that source.
     */
    public int getMisses(int entry) {
        return entry >= 0 && entry < misses.length ? misses[entry] : 0;
    }

    /**
     * @return how many items the quest still needs, never below 0.
     */
    public int getRemaining() {
        return Math.max(0, getTargetQuantity() - getCurrentQuantity());
    }

    /**
     * @return the source that applies to the victim: the highest chance among those whose group
     * holds it, the first on a tie, {@code -1} when none does.
     */
    public int matchingEntry(@Nonnull NPCEntity victim) {
        LootNpcDrop[] drops = getAsset().getDrops();
        int best = -1;

        for (int entry = 0; entry < drops.length; entry++) {
            int groupIndex = NPCGroup.getAssetMap().getIndex(drops[entry].getNpcGroupId());
            if (groupIndex < 0 || !TagSetPlugin.get(NPCGroup.class).tagInSet(groupIndex, victim.getNPCTypeIndex())) continue;

            if (best < 0 || drops[entry].getChance() > drops[best].getChance()) best = entry;
        }

        return best;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        Item item = asset == null ? null : Item.getAssetMap().getAsset(asset.getItemId());
        if (item == null) return super.getDefaultTitle();

        return Message.translation("openquests.quest.default.loot")
                      .param("quantity", getTargetQuantity())
                      .param("item", item.getTranslationMessage());
    }

    private boolean moveMisses(int entry, int value) {
        if (isOver() || entry < 0) return false;
        if (getMisses(entry) == value) return false;

        if (entry >= misses.length) misses = Arrays.copyOf(misses, entry + 1);
        misses[entry] = value;
        return true;
    }

    /**
     * A kill of that source's group that dropped nothing for this quest.
     */
    public record Miss(int entry) implements QuestOperation<LootNpcQuestProgression> {
        @Override
        public boolean applyTo(@Nonnull LootNpcQuestProgression quest) {
            return quest.moveMisses(entry, quest.getMisses(entry) + 1);
        }
    }

    /**
     * A drop that reached the player: the run of misses for that source starts over.
     */
    public record ResetMisses(int entry) implements QuestOperation<LootNpcQuestProgression> {
        @Override
        public boolean applyTo(@Nonnull LootNpcQuestProgression quest) {
            return quest.moveMisses(entry, 0);
        }
    }

    /**
     * Sets up a loot quest from its asset.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<LootNpcQuestProgression, Builder> {
        Builder(@Nonnull LootNpcQuestAsset asset) {
            this(asset.getId());
        }

        Builder(@Nonnull String assetId) {
            super(new LootNpcQuestProgression(), assetId);
        }
    }
}
