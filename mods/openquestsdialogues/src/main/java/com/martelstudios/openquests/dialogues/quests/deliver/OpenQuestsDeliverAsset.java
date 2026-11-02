package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Bring items to an NPC character, in one go or several. F on the character shows a delivery
 * screen before its normal behaviour while the player carries something still missing.
 */
public class OpenQuestsDeliverAsset extends QuantityQuestAsset {

    /**
     * One requested item and how many of it.
     */
    public static class DeliverItem {

        public static final BuilderCodec<DeliverItem> CODEC = BuilderCodec.builder(DeliverItem.class, DeliverItem::new)
                                                                           .append(new KeyedCodec<>("Item", QuestItemFilter.CODEC, true), (item, filter) -> item.item = filter, item -> item.item)
                                                                           .addValidator(Validators.nonNull())
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Quantity", Codec.INTEGER, true), (item, quantity) -> item.quantity = quantity, item -> item.quantity)
                                                                           .addValidator(Validators.min(1))
                                                                           .add()
                                                                           .append(new KeyedCodec<>("NameKey", Codec.STRING), (item, key) -> item.nameKey = key, item -> item.nameKey)
                                                                           .add()
                                                                           .build();

        protected QuestItemFilter item;

        protected int quantity = 1;

        @Nullable
        protected String nameKey;

        protected DeliverItem() {}

        public QuestItemFilter getItem() {
            return item;
        }

        public int getQuantity() {
            return quantity;
        }

        /**
         * @return the translation key naming the item to the player: the asset's own, else the item's
         * name, {@code null} for a tag nobody named.
         */
        @Nullable
        public String nameKey() {
            if (nameKey != null) return nameKey;
            Message name = item == null ? null : item.getName();
            return name == null ? null : name.getMessageId();
        }
    }

    public static final BuilderCodec<OpenQuestsDeliverAsset> CODEC =
        BuilderCodec.builder(OpenQuestsDeliverAsset.class, OpenQuestsDeliverAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Character", Codec.STRING, true), (asset, character) -> asset.character = character, asset -> asset.character)
                    .addValidator(Validators.nonNull())
                    .add()
                    .append(new KeyedCodec<>("Items", new ArrayCodec<>(DeliverItem.CODEC, DeliverItem[]::new), true), (asset, items) -> asset.items = items, asset -> asset.items)
                    .addValidator(Validators.nonEmptyArray())
                    .add()
                    .append(new KeyedCodec<>("Dialogue", Codec.STRING), (asset, dialogue) -> asset.dialogue = dialogue, asset -> asset.dialogue)
                    .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                    .add()
                    .build();

    protected String character;

    protected DeliverItem[] items = new DeliverItem[0];

    /**
     * The author's delivery dialogue, {@code null} for a generated one.
     */
    @Nullable
    protected String dialogue;

    OpenQuestsDeliverAsset() {}

    @Override
    public QuantityQuestProgression.Builder<DeliverQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new DeliverQuestProgression(), getId());
    }

    /**
     * The tracker counts units over every item, so its target is their sum.
     */
    @Override
    public int getTargetQuantity() {
        int total = 0;
        for (DeliverItem item : items) total += item.quantity;
        return total;
    }

    public String getCharacter() {
        return character;
    }

    @Nonnull
    public DeliverItem[] getItems() {
        return items;
    }

    @Nullable
    public String getDialogue() {
        return dialogue;
    }

    /**
     * @return each item's requested quantity, in order.
     */
    @Nonnull
    public int[] quantities() {
        int[] quantities = new int[items.length];
        for (int i = 0; i < items.length; i++) quantities[i] = items[i].quantity;
        return quantities;
    }
}
