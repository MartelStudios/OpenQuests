package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.dialogues.quests.dialogue.DialogueQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Counts what was delivered of each item; the inherited count is their total, which the tracker shows.
 */
public class DeliverQuestProgression extends QuantityQuestProgression<DeliverQuestProgression> {

    public static final BuilderCodec<DeliverQuestProgression> CODEC = BuilderCodec.builder(DeliverQuestProgression.class, DeliverQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("Delivered", Codec.INT_ARRAY), (quest, delivered) -> quest.delivered = delivered, quest -> quest.delivered)
                                                                                  .add()
                                                                                  .build();

    int[] delivered = new int[0];

    DeliverQuestProgression() {}

    @Override
    public OpenQuestsDeliverAsset getAsset() {
        return (OpenQuestsDeliverAsset) super.getAsset();
    }

    /**
     * @return how many of the item at that index were delivered, 0 for an index never delivered to.
     */
    public int delivered(int index) {
        return index >= 0 && index < delivered.length ? delivered[index] : 0;
    }

    /**
     * @return how many of the item at that index are still wanted.
     */
    public int missing(int index) {
        int[] quantities = getAsset().quantities();
        return index >= 0 && index < quantities.length ? Math.max(0, quantities[index] - delivered(index)) : 0;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getCharacter() == null) return super.getDefaultTitle();
        return Message.translation("openquestsdialogues.quest.default.deliver").param("quantity", getTargetQuantity()).param("character", DialogueQuestProgression.characterName(asset.getCharacter()));
    }

    /**
     * Adds to one item's count at most what it still misses, growing the array as items are met.
     *
     * @return how many were taken in, 0 for an index out of range or a complete item.
     */
    static int accept(@Nonnull int[] delivered, @Nonnull int[] quantities, int index, int amount) {
        if (index < 0 || index >= quantities.length || index >= delivered.length || amount <= 0) return 0;
        int taken = Math.min(amount, quantities[index] - delivered[index]);
        if (taken <= 0) return 0;
        delivered[index] += taken;
        return taken;
    }

    /**
     * Some of one item handed over. Capped on the copy it is made on, so a delivery racing another
     * never counts past the quantity.
     */
    public record Delivered(int index, int amount) implements QuestOperation<DeliverQuestProgression> {
        @Override
        public boolean applyTo(@Nonnull DeliverQuestProgression quest) {
            if (quest.isOver()) return false;
            int[] quantities = quest.getAsset().quantities();
            if (quest.delivered.length < quantities.length) quest.delivered = Arrays.copyOf(quest.delivered, quantities.length);

            int taken = accept(quest.delivered, quantities, index, amount);
            return taken > 0 && new Add(taken).applyTo(quest);
        }
    }
}
